package com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor;

import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.ConstructorPattern;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;
import com.jetbrains.jetpad.vclang.term.pattern.PatternExpansion;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.BranchElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.EmptyElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Index;
import static com.jetbrains.jetpad.vclang.term.pattern.PatternExpansion.expandPattern;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.expandPatternSubstitute;

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
  public Void visitBranch(final BranchElimTreeNode branchNode, List<Expression> expressions) {
    List<Expression> parameters = new ArrayList<>();
    Expression type = myContext.get(myContext.size() - 1 - branchNode.getIndex()).getType().liftIndex(0, branchNode.getIndex());
    DataDefinition dataType = ((DataCallExpression) type.normalize(NormalizeVisitor.Mode.WHNF, myContext).getFunction(parameters)).getDefinition();
    Collections.reverse(parameters);
    for (ConCallExpression conCall : dataType.getConstructors(parameters, myContext)) {
      if (branchNode.getChild(conCall.getDefinition()) != null) {
        try (ConCallContextExpander expander = new ConCallContextExpander(branchNode.getIndex(), conCall, myContext)) {
          branchNode.getChild(conCall.getDefinition()).accept(this, expander.substIn(expressions));
        }
      }
    }

    if (dataType.getConditions() != null) {
      for (Condition condition : dataType.getConditions()) {
        if (branchNode.getChild(condition.getConstructor()) != null) {
          List<Binding> tail = new ArrayList<>(myContext.subList(myContext.size() - 1 - branchNode.getIndex(), myContext.size()));
          myContext.subList(myContext.size() - 1 - branchNode.getIndex(), myContext.size()).clear();
          Pattern pattern = new ConstructorPattern(condition.getConstructor(), condition.getPatterns(), true);
          final PatternExpansion.Result expansionResult = expandPattern(pattern, type.liftIndex(0, -branchNode.getIndex()), myContext);
          for (TypeArgument arg : expansionResult.args) {
            myContext.add(new TypedBinding((String) null, arg.getType()));
          }
          for (int i = 1; i < tail.size(); i++) {
            myContext.add(new TypedBinding((String) null, expandPatternSubstitute(pattern, i - 1, expansionResult.expression.getExpression().liftIndex(0, i - 1), tail.get(i).getType())));
          }

          List<Expression> subst = new ArrayList<>();
          for (int i = 0; i < tail.size() - 1; i++) {
            subst.add(Index(i));
          }
          subst.add(expansionResult.expression.getExpression().liftIndex(0, branchNode.getIndex()));

          List<Expression> newExpressions = new ArrayList<>(expressions.size());
          for (Expression expr : expressions) {
            newExpressions.add(expandPatternSubstitute(pattern, branchNode.getIndex(), expansionResult.expression.getExpression().liftIndex(0, branchNode.getIndex()), expr));
          }
          for (Expression expr : expressions) {
            newExpressions.add(expandPatternSubstitute(pattern, branchNode.getIndex(), condition.getTerm().liftIndex(0, branchNode.getIndex()), expr));
          }
          for (int i = 0; i < tail.size() - 1; i++) {
            newExpressions.add(Index(i));
          }
          newExpressions.add(condition.getTerm().liftIndex(0, branchNode.getIndex()));


          SubstituteExpander.substituteExpand(myContext, subst, branchNode, newExpressions, new SubstituteExpander.SubstituteExpansionProcessor() {
            @Override
            public void process(List<Expression> expressions, List<Binding> context, final List<Expression> subst, LeafElimTreeNode leaf) {
              List<Expression> newSubst = new ArrayList<>(expressions.subList(expressions.size() - 1 - branchNode.getIndex(), expressions.size()));
              expressions.subList(expressions.size() - 1 - branchNode.getIndex(), expressions.size()).clear();
              expressions.add(leaf.getExpression().subst(subst, 0));
              SubstituteExpander.substituteExpand(context, newSubst, branchNode, expressions, new SubstituteExpander.SubstituteExpansionProcessor() {
                @Override
                public void process(List<Expression> expressions, List<Binding> context, List<Expression> subst, LeafElimTreeNode leaf) {
                  Expression lhs = expressions.get(expressions.size() - 1);
                  expressions.remove(expressions.size() - 1);
                  Expression rhs = leaf.getExpression().subst(subst, 0);
                  myChecker.check(context, lhs, new ArrayList<>(expressions.subList(0, expressions.size() / 2)), rhs, new ArrayList<>(expressions.subList(expressions.size() / 2, expressions.size())));
                }
              });
            }
          });
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
