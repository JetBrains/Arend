package com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor;

import com.jetbrains.jetpad.vclang.term.context.Utils;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.BranchElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.EmptyElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.toContext;
import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.toSubstitution;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Apps;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Reference;


public class SubstituteExpander {
  public interface SubstituteExpansionProcessor {
    void process(Substitution subst, Substitution toCtx, List<Binding> ctx, LeafElimTreeNode leaf);
  }

  private final SubstituteExpansionProcessor myProcessor;
  private final List<Binding> myContext;

  private SubstituteExpander(SubstituteExpansionProcessor processor, List<Binding> context) {
    myProcessor = processor;
    myContext = context;
  }

  public static void substituteExpand(ElimTreeNode tree, final Substitution subst, List<Binding> context, SubstituteExpansionProcessor processor) {
    Substitution toCtx = new Substitution();
    for (Binding binding : context) {
      toCtx.addMapping(binding, Reference(binding));
    }
    new SubstituteExpander(processor, context).substituteExpand(tree, subst, toCtx);
  }

  private void substituteExpand(ElimTreeNode tree, final Substitution subst, final Substitution toCtx) {
    tree.matchUntilStuck(subst).accept(new ElimTreeNodeVisitor<Void, Void>() {
      @Override
      public Void visitBranch(BranchElimTreeNode branchNode, Void params) {
        if (!(subst.get(branchNode.getReference()) instanceof ReferenceExpression)) {
          return null;
        }
        Binding binding = ((ReferenceExpression) subst.get(branchNode.getReference())).getBinding();
        List<Expression> parameters = new ArrayList<>();
        Expression ftype = binding.getType().normalize(NormalizeVisitor.Mode.WHNF).getFunction(parameters);
        Collections.reverse(parameters);

        for (ConCallExpression conCall : ((DataCallExpression) ftype).getDefinition().getMatchedConstructors(parameters)) {
          DependentLink constructorArgs = conCall.getDefinition().getParameters().subst(toSubstitution(conCall.getDefinition().getDataTypeParameters(), conCall.getDataTypeArguments()));
          Expression substExpr = conCall;
          for (DependentLink link = constructorArgs; link.hasNext(); link = link.getNext()) {
            substExpr = Apps(substExpr, Reference(link));
          }

          List<Binding> tail = new ArrayList<>(myContext.subList(myContext.lastIndexOf(binding) + 1, myContext.size()));
          myContext.subList(myContext.lastIndexOf(binding), myContext.size()).clear();
          try (Utils.ContextSaver ignore = new Utils.ContextSaver(myContext)) {
            Substitution currentSubst = new Substitution(binding, substExpr);
            myContext.addAll(toContext(constructorArgs));
            myContext.addAll(currentSubst.extendBy(tail));
            substituteExpand(branchNode, currentSubst.compose(subst), currentSubst.compose(toCtx));
          }
          myContext.add(binding);
          myContext.addAll(tail);
        }

        return null;
      }

      @Override
      public Void visitLeaf(LeafElimTreeNode leafNode, Void params) {
        myProcessor.process(subst, toCtx, myContext, leafNode);
        return null;
      }

      @Override
      public Void visitEmpty(EmptyElimTreeNode emptyNode, Void params) {
        return null;
      }
    }, null);
  }
}
