package com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor;

import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.Utils;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.*;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.toContext;
import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.toSubstitution;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Left;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Right;

public class ConditionViolationsCollector implements ElimTreeNodeVisitor<ExprSubstitution, Void>  {
  public interface ConditionViolationChecker {
    void check(Expression expr1, ExprSubstitution argSubst1, Expression expr2, ExprSubstitution argSubst2);
  }

  private final ConditionViolationChecker myChecker;

  private ConditionViolationsCollector(ConditionViolationChecker checker) {
    myChecker = checker;
  }

  public static void check(ElimTreeNode tree, ExprSubstitution argSubst, ConditionViolationChecker checker) {
    tree.accept(new ConditionViolationsCollector(checker), argSubst);
  }

  @Override
  public Void visitBranch(final BranchElimTreeNode branchNode, final ExprSubstitution argSubst) {
    Expression type = branchNode.getReference().getType().normalize(NormalizeVisitor.Mode.WHNF);
    for (final ConCallExpression conCall : type.toDataCall().getDefinition().getMatchedConstructors(type.toDataCall().getDefCallArguments())) {
      if (branchNode.getClause(conCall.getDefinition()) != null) {
        Clause clause = branchNode.getClause(conCall.getDefinition());
        clause.getChild().accept(this, clause.getSubst().compose(argSubst));
        if (conCall.getDefinition().getDataType().getCondition(conCall.getDefinition()) != null) {
          ExprSubstitution subst = toSubstitution(conCall.getDefinition().getDataTypeParameters(), conCall.getDataTypeArguments());
          final DependentLink constructorArgs = DependentLink.Helper.subst(conCall.getDefinition().getParameters(), subst);
          SubstituteExpander.substituteExpand(conCall.getDefinition().getDataType().getCondition(conCall.getDefinition()).getElimTree(), subst, toContext(constructorArgs), new SubstituteExpander.SubstituteExpansionProcessor() {
            @Override
            public void process(ExprSubstitution subst, ExprSubstitution toCtxC, List<Binding> ctx, LeafElimTreeNode leaf) {
              for (DependentLink link = constructorArgs; link.hasNext(); link = link.getNext()) {
                conCall.addArgument(toCtxC.get(link));
              }
              final Expression rhs = leaf.getExpression().subst(subst);
              try (Utils.ContextSaver ignore = new Utils.ContextSaver(ctx)) {
                final ExprSubstitution subst1 = new ExprSubstitution(branchNode.getReference(), conCall);
                ctx.addAll(subst1.extendBy(branchNode.getContextTail()));
                SubstituteExpander.substituteExpand(branchNode, subst1, ctx, new SubstituteExpander.SubstituteExpansionProcessor() {
                  @Override
                  public void process(ExprSubstitution subst, final ExprSubstitution toCtx1, List<Binding> ctx, LeafElimTreeNode leaf) {
                    final Expression lhsVal = leaf.getExpression().subst(subst);
                    try (Utils.ContextSaver ignore = new Utils.ContextSaver(ctx)) {
                      final ExprSubstitution subst2 = new ExprSubstitution(branchNode.getReference(), rhs.subst(toCtx1));
                      for (Binding binding : branchNode.getContextTail()) {
                        subst2.add(binding, subst1.get(binding).subst(toCtx1));
                      }
                      SubstituteExpander.substituteExpand(branchNode, subst2, ctx, new SubstituteExpander.SubstituteExpansionProcessor() {
                        @Override
                        public void process(ExprSubstitution subst, ExprSubstitution toCtx2, List<Binding> ctx, LeafElimTreeNode leaf) {
                          myChecker.check(lhsVal.subst(toCtx2), toCtx2.compose(toCtx1.compose(subst1.compose(argSubst))),
                              leaf.getExpression().subst(subst), toCtx2.compose(subst2.compose(argSubst)));
                        }
                      });
                    }
                  }
                });
              }
            }
          });
        }
        if (conCall.getDefinition() == Prelude.ABSTRACT) {
          if (branchNode.getClause(Prelude.LEFT) != branchNode.getOtherwiseClause()) {
            checkInterval(branchNode, argSubst, Left());
          }
          if (branchNode.getClause(Prelude.RIGHT) != branchNode.getOtherwiseClause()) {
            checkInterval(branchNode, argSubst, Right());
          }
        }
      }
    }
    return null;
  }

  private void checkInterval(final BranchElimTreeNode branchNode, final ExprSubstitution argSubst, final ConCallExpression conCall) {
    final ExprSubstitution subst1 = new ExprSubstitution(branchNode.getReference(), conCall);
    List<Binding> ctx = subst1.extendBy(branchNode.getContextTail());
    SubstituteExpander.substituteExpand(branchNode, subst1, ctx, new SubstituteExpander.SubstituteExpansionProcessor() {
      @Override
      public void process(ExprSubstitution subst, final ExprSubstitution toCtx1, List<Binding> ctx, LeafElimTreeNode leaf) {
        final Expression lhsVal = leaf.getExpression().subst(subst);
        try (Utils.ContextSaver ignore = new Utils.ContextSaver(ctx)) {
          final ExprSubstitution subst2 = new ExprSubstitution(branchNode.getReference(), conCall);
          ctx.addAll(subst2.extendBy(branchNode.getContextTail()));
          SubstituteExpander.substituteExpand(branchNode.getOtherwiseClause().getChild(), subst2, ctx, new SubstituteExpander.SubstituteExpansionProcessor() {
            @Override
            public void process(ExprSubstitution subst, ExprSubstitution toCtx2, List<Binding> ctx, LeafElimTreeNode leaf) {
              myChecker.check(lhsVal.subst(toCtx2), toCtx2.compose(toCtx1.compose(subst1.compose(argSubst))),
                  leaf.getExpression().subst(subst), toCtx2.compose(subst2.compose(argSubst)));
            }
          });
        }
      }
    });
  }

  @Override
  public Void visitLeaf(LeafElimTreeNode leafNode, ExprSubstitution argSubst) {
    return null;
  }

  @Override
  public Void visitEmpty(EmptyElimTreeNode emptyNode, ExprSubstitution argSubst) {
    return null;
  }
}
