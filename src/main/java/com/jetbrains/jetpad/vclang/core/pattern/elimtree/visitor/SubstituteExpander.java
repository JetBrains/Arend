package com.jetbrains.jetpad.vclang.core.pattern.elimtree.visitor;

import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.BranchElimTreeNode;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.EmptyElimTreeNode;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.LeafElimTreeNode;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.core.context.param.DependentLink.Helper.toContext;
import static com.jetbrains.jetpad.vclang.core.context.param.DependentLink.Helper.toSubstitution;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.Reference;


public class SubstituteExpander {
  public interface SubstituteExpansionProcessor {
    void process(ExprSubstitution subst, ExprSubstitution toCtx, List<Binding> ctx, LeafElimTreeNode leaf);
  }

  private final SubstituteExpansionProcessor myProcessor;
  private final List<Binding> myContext;

  private SubstituteExpander(SubstituteExpansionProcessor processor, List<Binding> context) {
    myProcessor = processor;
    myContext = context;
  }

  public static void substituteExpand(ElimTreeNode tree, final ExprSubstitution subst, List<? extends Binding> context, SubstituteExpansionProcessor processor) {
    ExprSubstitution toCtx = new ExprSubstitution();
    for (Binding binding : context) {
      toCtx.add(binding, Reference(binding));
    }
    new SubstituteExpander(processor, new ArrayList<>(context)).substituteExpand(tree, subst, toCtx);
  }

  private void substituteExpand(ElimTreeNode tree, ExprSubstitution subst, final ExprSubstitution toCtx) {
    final ExprSubstitution nestedSubstitution = new ExprSubstitution().compose(subst);
    tree.matchUntilStuck(nestedSubstitution, false).accept(new ElimTreeNodeVisitor<Void, Void>() {
      @Override
      public Void visitBranch(BranchElimTreeNode branchNode, Void params) {
        ReferenceExpression reference = nestedSubstitution.get(branchNode.getReference()).toReference();
        if (reference == null) {
          return null;
        }
        Binding binding = reference.getBinding();
        DataCallExpression dType = binding.getType().getExpr().normalize(NormalizeVisitor.Mode.WHNF).toDataCall();

        for (ConCallExpression conCall : dType.getDefinition().getMatchedConstructors(dType)) {
          DependentLink constructorArgs = DependentLink.Helper.subst(conCall.getDefinition().getParameters(), toSubstitution(conCall.getDefinition().getDataTypeParameters(), conCall.getDataTypeArguments()));
          for (DependentLink link = constructorArgs; link.hasNext(); link = link.getNext()) {
            conCall.addArgument(Reference(link));
          }

          List<Binding> tail = new ArrayList<>(myContext.subList(myContext.lastIndexOf(binding) + 1, myContext.size()));
          myContext.subList(myContext.lastIndexOf(binding), myContext.size()).clear();
          try (Utils.ContextSaver ignore = new Utils.ContextSaver(myContext)) {
            ExprSubstitution currentSubst = new ExprSubstitution(binding, conCall);
            myContext.addAll(toContext(constructorArgs));
            myContext.addAll(currentSubst.extendBy(tail));
            substituteExpand(branchNode, currentSubst.compose(nestedSubstitution), currentSubst.compose(toCtx));
          }
          myContext.add(binding);
          myContext.addAll(tail);
        }

        return null;
      }

      @Override
      public Void visitLeaf(LeafElimTreeNode leafNode, Void params) {
        myProcessor.process(nestedSubstitution, toCtx, myContext, leafNode);
        return null;
      }

      @Override
      public Void visitEmpty(EmptyElimTreeNode emptyNode, Void params) {
        return null;
      }
    }, null);
  }
}
