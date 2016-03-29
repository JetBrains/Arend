package com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor;

import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.context.Utils;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.term.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.Substitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.*;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.toContext;
import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.toSubstitution;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class ConditionViolationsCollector implements ElimTreeNodeVisitor<Substitution, Void>  {
  public interface ConditionViolationChecker {
    void check(Expression expr1, Substitution argSubst1, Expression expr2, Substitution argSubst2);
  }

  private final ConditionViolationChecker myChecker;

  private ConditionViolationsCollector(ConditionViolationChecker checker) {
    myChecker = checker;
  }

  public static void check(ElimTreeNode tree, Substitution argSubst, ConditionViolationChecker checker) {
    tree.accept(new ConditionViolationsCollector(checker), argSubst);
  }

  @Override
  public Void visitBranch(final BranchElimTreeNode branchNode, final Substitution argSubst) {
    Expression type = branchNode.getReference().getType().normalize(NormalizeVisitor.Mode.WHNF);
    List<? extends Expression> parameters = type.getArguments();
    DataDefinition dataType = type.getFunction().toDataCall().getDefinition();

    for (final ConCallExpression conCall : dataType.getMatchedConstructors(parameters)) {
      if (branchNode.getClause(conCall.getDefinition()) != null) {
        Clause clause = branchNode.getClause(conCall.getDefinition());
        clause.getChild().accept(this, clause.getSubst().compose(argSubst));
        if (conCall.getDefinition().getDataType().getCondition(conCall.getDefinition()) != null) {
          Substitution subst = toSubstitution(conCall.getDefinition().getDataTypeParameters(), conCall.getDataTypeArguments());
          final DependentLink constructorArgs = DependentLink.Helper.subst(conCall.getDefinition().getParameters(), subst);
          SubstituteExpander.substituteExpand(conCall.getDefinition().getDataType().getCondition(conCall.getDefinition()).getElimTree(), subst, toContext(constructorArgs), new SubstituteExpander.SubstituteExpansionProcessor() {
            @Override
            public void process(Substitution subst, Substitution toCtxC, List<Binding> ctx, LeafElimTreeNode leaf) {
              List<Expression> arguments = new ArrayList<>();
              for (DependentLink link = constructorArgs; link.hasNext(); link = link.getNext()) {
                arguments.add(toCtxC.get(link));
              }
              final Expression rhs = leaf.getExpression().subst(subst);
              try (Utils.ContextSaver ignore = new Utils.ContextSaver(ctx)) {
                final Substitution subst1 = new Substitution(branchNode.getReference(), Apps(conCall, arguments));
                ctx.addAll(subst1.extendBy(branchNode.getContextTail()));
                SubstituteExpander.substituteExpand(branchNode, subst1, ctx, new SubstituteExpander.SubstituteExpansionProcessor() {
                  @Override
                  public void process(Substitution subst, final Substitution toCtx1, List<Binding> ctx, LeafElimTreeNode leaf) {
                    final Expression lhsVal = leaf.getExpression().subst(subst);
                    try (Utils.ContextSaver ignore = new Utils.ContextSaver(ctx)) {
                      final Substitution subst2 = new Substitution(branchNode.getReference(), rhs.subst(toCtx1));
                      for (Binding binding : branchNode.getContextTail()) {
                        subst2.add(binding, subst1.get(binding).subst(toCtx1));
                      }
                      SubstituteExpander.substituteExpand(branchNode, subst2, ctx, new SubstituteExpander.SubstituteExpansionProcessor() {
                        @Override
                        public void process(Substitution subst, Substitution toCtx2, List<Binding> ctx, LeafElimTreeNode leaf) {
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
        if (conCall.getDefinition() == Preprelude.ABSTRACT) {
          if (branchNode.getClause(Preprelude.LEFT) != branchNode.getOtherwiseClause()) {
            checkInterval(branchNode, argSubst, Left());
          }
          if (branchNode.getClause(Preprelude.RIGHT) != branchNode.getOtherwiseClause()) {
            checkInterval(branchNode, argSubst, Right());
          }
        }
      }
    }
    return null;
  }

  private void checkInterval(final BranchElimTreeNode branchNode, final Substitution argSubst, final ConCallExpression conCall) {
    final Substitution subst1 = new Substitution(branchNode.getReference(), conCall);
    List<Binding> ctx = subst1.extendBy(branchNode.getContextTail());
    SubstituteExpander.substituteExpand(branchNode, subst1, ctx, new SubstituteExpander.SubstituteExpansionProcessor() {
      @Override
      public void process(Substitution subst, final Substitution toCtx1, List<Binding> ctx, LeafElimTreeNode leaf) {
        final Expression lhsVal = leaf.getExpression().subst(subst);
        try (Utils.ContextSaver ignore = new Utils.ContextSaver(ctx)) {
          final Substitution subst2 = new Substitution(branchNode.getReference(), conCall);
          ctx.addAll(subst2.extendBy(branchNode.getContextTail()));
          SubstituteExpander.substituteExpand(branchNode.getOtherwiseClause().getChild(), subst2, ctx, new SubstituteExpander.SubstituteExpansionProcessor() {
            @Override
            public void process(Substitution subst, Substitution toCtx2, List<Binding> ctx, LeafElimTreeNode leaf) {
              myChecker.check(lhsVal.subst(toCtx2), toCtx2.compose(toCtx1.compose(subst1.compose(argSubst))),
                  leaf.getExpression().subst(subst), toCtx2.compose(subst2.compose(argSubst)));
            }
          });
        }
      }
    });
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
