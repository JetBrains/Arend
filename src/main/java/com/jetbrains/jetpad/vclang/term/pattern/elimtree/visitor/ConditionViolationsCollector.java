package com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor;

import com.jetbrains.jetpad.vclang.term.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.term.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.param.Binding;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.BranchElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.EmptyElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Index;
import static com.jetbrains.jetpad.vclang.term.expr.param.Utils.splitArguments;

public class ConditionViolationsCollector implements ElimTreeNodeVisitor<List<Expression>, Void>  {
  public interface ConditionViolationChecker {
    void check(List<Binding> context, Expression expr1, List<Expression> subst1, Expression expr2, List<Expression> subst2);
  }

  private final List<Binding> myContext;
  private final ConditionViolationChecker myChecker;

  private ConditionViolationsCollector(List<Binding> context, ConditionViolationChecker checker) {
    myContext = context;
    myChecker = checker;
  }

  public static void check(List<Binding> context, ElimTreeNode tree, ConditionViolationChecker checker, int argsStartIndex) {
    List<Expression> expressions = new ArrayList<>(context.size() - argsStartIndex);
      for (int i = context.size() - argsStartIndex - 1; i >= 0; i--) {
        expressions.add(Index(i));
    }
    tree.accept(new ConditionViolationsCollector(context, checker), expressions);
  }

  @Override
  public Void visitBranch(final BranchElimTreeNode branchNode, final List<Expression> expressions) {
    List<Expression> parameters = new ArrayList<>();
    Expression type = myContext.get(myContext.size() - 1 - branchNode.getIndex()).getType().liftIndex(0, branchNode.getIndex());
    DataDefinition dataType = ((DataCallExpression) type.normalize(NormalizeVisitor.Mode.WHNF, myContext).getFunction(parameters)).getDefinition();
    Collections.reverse(parameters);
    for (ConCallExpression conCall : dataType.getConstructors(parameters, myContext)) {
      if (branchNode.getChild(conCall.getDefinition()) != null) {
        final int numBindingsBefore = myContext.size() - branchNode.getIndex() - 1;
        try (ConCallContextExpander expander = new ConCallContextExpander(branchNode.getIndex(), conCall, myContext)) {
          branchNode.getChild(conCall.getDefinition()).accept(this, expander.substIn(expressions));
          if (conCall.getDefinition().getDataType().getCondition(conCall.getDefinition()) != null) {
            final int numConstructorArguments = splitArguments(conCall.getDefinition().getArguments()).size();
            List<Expression> subst = new ArrayList<>(numConstructorArguments);
            for (int i = 0; i < numConstructorArguments; i++) {
              subst.add(Index(branchNode.getIndex() + i));
            }
            SubstituteExpander.substituteExpand(myContext, subst, conCall.getDefinition().getDataType().getCondition(conCall.getDefinition()).getElimTree(),
                new ArrayList<>(Collections.singletonList(expander.substIn(Index(branchNode.getIndex())))), new SubstituteExpander.SubstituteExpansionProcessor() {
              @Override
              public void process(List<Expression> newConCall, List<Binding> context, List<Expression> subst, LeafElimTreeNode leaf) {
                List<Expression> newSubst = new ArrayList<>();
                for (int i = 0; i < branchNode.getIndex(); i++) {
                  newSubst.add(Index(i));
                }
                newSubst.add(newConCall.get(0));

                List<Expression> newExpressions = new ArrayList<>(expressions.size());
                for (Expression expr : expressions) {
                  newExpressions.add(expr.liftIndex(branchNode.getIndex() + 1, subst.size()).subst(newConCall.get(0), branchNode.getIndex()));
                }
                for (Expression expr : expressions) {
                  newExpressions.add(expr.liftIndex(branchNode.getIndex() + 1, subst.size()).subst(leaf.getExpression().liftIndex(0, branchNode.getIndex()), branchNode.getIndex()));
                }
                for (int i = 0; i < branchNode.getIndex(); i++) {
                  newExpressions.add(Index(i));
                }
                newExpressions.add(leaf.getExpression().liftIndex(0, branchNode.getIndex()));

                SubstituteExpander.substituteExpand(myContext, newSubst, branchNode, newExpressions, new SubstituteExpander.SubstituteExpansionProcessor() {
                  @Override
                  public void process(List<Expression> expressions, List<Binding> context, final List<Expression> subst, LeafElimTreeNode leaf) {
                    List<Expression> newSubst = new ArrayList<>(expressions.subList(expressions.size() - 1 - branchNode.getIndex(), expressions.size()));
                    expressions.subList(expressions.size() - 1 - branchNode.getIndex(), expressions.size()).clear();
                    expressions.add(leaf.getExpression().liftIndex(subst.size(), context.size() - numBindingsBefore).subst(subst, 0));
                    SubstituteExpander.substituteExpand(context, newSubst, branchNode, expressions, new SubstituteExpander.SubstituteExpansionProcessor() {
                      @Override
                      public void process(List<Expression> expressions, List<Binding> context, List<Expression> subst, LeafElimTreeNode leaf) {
                        Expression lhs = expressions.get(expressions.size() - 1);
                        expressions.remove(expressions.size() - 1);
                        Expression rhs = leaf.getExpression().liftIndex(subst.size(), context.size() - numBindingsBefore).subst(subst, 0);
                        myChecker.check(context, lhs, new ArrayList<>(expressions.subList(0, expressions.size() / 2)), rhs, new ArrayList<>(expressions.subList(expressions.size() / 2, expressions.size())));
                      }
                    });
                  }
                });
              }
            });
          }
        }
      }
    }
    return null;
  }

  @Override
  public Void visitLeaf(LeafElimTreeNode leafNode, List<Expression> expressions) {
    return null;
  }

  @Override
  public Void visitEmpty(EmptyElimTreeNode emptyNode, List<Expression> params) {
    return null;
  }
}
