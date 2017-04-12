package com.jetbrains.jetpad.vclang.core.pattern.elimtree.visitor;

import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.*;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.Prelude;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.core.context.param.DependentLink.Helper.toContext;
import static com.jetbrains.jetpad.vclang.core.context.param.DependentLink.Helper.toSubstitution;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.Left;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.Right;

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
    DataCallExpression type = branchNode.getReference().getType().getExpr().normalize(NormalizeVisitor.Mode.WHNF).toDataCall();
    for (final ConCallExpression conCall : type.getDefinition().getMatchedConstructors(type)) {
      if (branchNode.getClause(conCall.getDefinition()) != null) {
        Clause clause = branchNode.getClause(conCall.getDefinition());
        clause.getChild().accept(this, clause.getSubst().compose(argSubst));
        if (conCall.getDefinition().getCondition() != null) {
          ExprSubstitution subst = toSubstitution(conCall.getDefinition().getDataTypeParameters(), conCall.getDataTypeArguments());
          final DependentLink constructorArgs = DependentLink.Helper.subst(conCall.getDefinition().getParameters(), subst);
          SubstituteExpander.substituteExpand(conCall.getDefinition().getCondition(), subst, toContext(constructorArgs), (subst1, toCtxC, ctx, leaf) -> {
            List<Expression> arguments = new ArrayList<>();
            for (DependentLink link = constructorArgs; link.hasNext(); link = link.getNext()) {
              arguments.add(toCtxC.get(link));
            }
            final Expression rhs = leaf.getExpression().subst(subst1);
            try (Utils.ContextSaver ignore = new Utils.ContextSaver(ctx)) {
              final ExprSubstitution subst2 = new ExprSubstitution(branchNode.getReference(), new ConCallExpression(conCall.getDefinition(), conCall.getSortArgument(), new ArrayList<>(conCall.getDataTypeArguments()), arguments));
              ctx.addAll(subst2.extendBy(branchNode.getContextTail()));
              SubstituteExpander.substituteExpand(branchNode, subst2, ctx, (subst3, toCtx1, ctx1, leaf1) -> {
                final Expression lhsVal = leaf1.getExpression().subst(subst3);
                try (Utils.ContextSaver ignore1 = new Utils.ContextSaver(ctx1)) {
                  final ExprSubstitution subst4 = new ExprSubstitution(branchNode.getReference(), rhs.subst(toCtx1));
                  for (Binding binding : branchNode.getContextTail()) {
                    subst4.add(binding, subst2.get(binding).subst(toCtx1));
                  }
                  SubstituteExpander.substituteExpand(branchNode, subst4, ctx1, (subst5, toCtx2, ctx11, leaf11) -> myChecker.check(lhsVal.subst(toCtx2), toCtx2.compose(toCtx1.compose(subst2.compose(argSubst))),
                      leaf11.getExpression().subst(subst5), toCtx2.compose(subst4.compose(argSubst))));
                }
              });
            }
          });
        }
        // TODO: Remove abstract
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
    List<TypedBinding> ctx = subst1.extendBy(branchNode.getContextTail());
    SubstituteExpander.substituteExpand(branchNode, subst1, ctx, (subst, toCtx1, ctx1, leaf) -> {
      final Expression lhsVal = leaf.getExpression().subst(subst);
      try (Utils.ContextSaver ignore = new Utils.ContextSaver(ctx1)) {
        final ExprSubstitution subst2 = new ExprSubstitution(branchNode.getReference(), conCall);
        ctx1.addAll(subst2.extendBy(branchNode.getContextTail()));
        SubstituteExpander.substituteExpand(branchNode.getOtherwiseClause().getChild(), subst2, ctx1, (subst3, toCtx2, ctx2, leaf1) -> myChecker.check(lhsVal.subst(toCtx2), toCtx2.compose(toCtx1.compose(subst1.compose(argSubst))),
            leaf1.getExpression().subst(subst3), toCtx2.compose(subst2.compose(argSubst))));
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
