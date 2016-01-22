package com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor;

import com.jetbrains.jetpad.vclang.term.context.Utils;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.toContext;
import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.toSubstitution;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Apps;

public class ConditionViolationsCollector implements ElimTreeNodeVisitor<Substitution, Void>  {
  public interface ConditionViolationChecker {
    void check(Expression expr1, Substitution argSubst1, Expression expr2, Substitution argSubst2);
  }

  private final ConditionViolationChecker myChecker;

  private ConditionViolationsCollector(ConditionViolationChecker checker) {
    myChecker = checker;
  }

  public static void check(ElimTreeNode tree, ConditionViolationChecker checker, Substitution argSubst) {
    tree.accept(new ConditionViolationsCollector(checker), argSubst);
  }

  @Override
  public Void visitBranch(final BranchElimTreeNode branchNode, final Substitution argSubst) {
    List<Expression> parameters = new ArrayList<>();
    Expression type = branchNode.getReference().getType();
    DataDefinition dataType = ((DataCallExpression) type.normalize(NormalizeVisitor.Mode.WHNF).getFunction(parameters)).getDefinition();
    Collections.reverse(parameters);

    for (final ConCallExpression conCall : dataType.getMatchedConstructors(parameters)) {
      if (branchNode.getClause(conCall.getDefinition()) != null) {
        ConstructorClause clause = branchNode.getClause(conCall.getDefinition());
        clause.getChild().accept(this, clause.getSubst().subst(argSubst));
        if (conCall.getDefinition().getDataType().getCondition(conCall.getDefinition()) != null) {
          Substitution subst = toSubstitution(conCall.getDefinition().getDataTypeParameters(), conCall.getDataTypeArguments());
          final DependentLink constructorArgs = conCall.getDefinition().getParameters().subst(subst);
          SubstituteExpander.substituteExpand(conCall.getDefinition().getDataType().getCondition(conCall.getDefinition()).getElimTree(), subst, toContext(constructorArgs), new SubstituteExpander.SubstituteExpansionProcessor() {
            @Override
            public void process(Substitution subst, final Substitution toCtxC, List<Binding> ctx, LeafElimTreeNode leaf) {
              Expression lhs = conCall;
              for (DependentLink link = constructorArgs; link != null; link = link.getNext()) {
                lhs = Apps(lhs, toCtxC.get(link));
              }
              final Expression rhs = leaf.getExpression().subst(subst);
              try (Utils.ContextSaver ignore = new Utils.ContextSaver(ctx)) {
                final Substitution subst1 = new Substitution(branchNode.getReference(), rhs);
                ctx.addAll(subst1.extendBy(branchNode.getContextTail()));
                SubstituteExpander.substituteExpand(branchNode, subst1, ctx, new SubstituteExpander.SubstituteExpansionProcessor() {
                  @Override
                  public void process(Substitution subst, final Substitution toCtx1, List<Binding> ctx, LeafElimTreeNode leaf) {
                    final Expression lhsVal = leaf.getExpression().subst(subst);
                    try (Utils.ContextSaver ignore = new Utils.ContextSaver(ctx)) {
                      final Substitution subst2 = new Substitution(branchNode.getReference(), rhs.subst(toCtx1));
                      ctx.addAll(subst2.extendBy(branchNode.getContextTail()));
                      SubstituteExpander.substituteExpand(branchNode, subst2, ctx, new SubstituteExpander.SubstituteExpansionProcessor() {
                        @Override
                        public void process(Substitution subst, Substitution toCtx2, List<Binding> ctx, LeafElimTreeNode leaf) {
                          myChecker.check(lhsVal.subst(toCtx2), toCtx2.subst(toCtx1.subst(subst1.subst(argSubst))),
                              leaf.getExpression().subst(subst), toCtx2.subst(subst2.subst(argSubst)));
                        }
                      });
                    }
                  }
                });
              }
            }
          });
        }
      }
    }
    return null;
  }

  @Override
  public Void visitLeaf(LeafElimTreeNode leafNode, Substitution argSubst) {
    return null;
  }

  @Override
  public Void visitEmpty(EmptyElimTreeNode emptyNode, Substitution argSubst) {
    return null;
  }
}
