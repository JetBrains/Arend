package com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor;

import com.jetbrains.jetpad.vclang.term.context.Utils;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.term.expr.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.BranchElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.EmptyElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.toContext;
import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.toSubstitution;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Apps;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Reference;


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

  public static void substituteExpand(ElimTreeNode tree, final ExprSubstitution subst, List<Binding> context, SubstituteExpansionProcessor processor) {
    ExprSubstitution toCtx = new ExprSubstitution();
    for (Binding binding : context) {
      toCtx.add(binding, Reference(binding));
    }
    new SubstituteExpander(processor, context).substituteExpand(tree, subst, toCtx);
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
        Expression ftype = binding.getType().normalize(NormalizeVisitor.Mode.WHNF);
        List<? extends Expression> arguments = ftype.getArguments();
        ftype = ftype.getFunction();

        for (ConCallExpression conCall : ftype.toDataCall().getDefinition().getMatchedConstructors(arguments)) {
          DependentLink constructorArgs = DependentLink.Helper.subst(conCall.getDefinition().getParameters(), toSubstitution(conCall.getDefinition().getDataTypeParameters(), conCall.getDataTypeArguments()));
          List<Expression> args = new ArrayList<>();
          for (DependentLink link = constructorArgs; link.hasNext(); link = link.getNext()) {
            args.add(Reference(link));
          }

          List<Binding> tail = new ArrayList<>(myContext.subList(myContext.lastIndexOf(binding) + 1, myContext.size()));
          myContext.subList(myContext.lastIndexOf(binding), myContext.size()).clear();
          try (Utils.ContextSaver ignore = new Utils.ContextSaver(myContext)) {
            ExprSubstitution currentSubst = new ExprSubstitution(binding, Apps(conCall, args));
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
