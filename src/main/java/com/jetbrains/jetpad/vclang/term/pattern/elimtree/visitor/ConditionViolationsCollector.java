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
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Index;
import static com.jetbrains.jetpad.vclang.term.pattern.PatternExpansion.expandPattern;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.expandPatternSubstitute;

public class ConditionViolationsCollector<T> implements ElimTreeNodeVisitor<T, Void, List<Expression>>  {
  public interface ConditionViolationChecker<T> {
    void check(List<Binding> context, T v1, List<Expression> subst1, T v2, List<Expression> subst2);
  }

  private final List<Binding> myContext;
  private final ConditionViolationChecker<T> myChecker;

  private ConditionViolationsCollector(List<Binding> context, ConditionViolationChecker<T> checker) {
    myContext = context;
    myChecker = checker;
  }

  public static <T> void check(List<Binding> context, ElimTreeNode<T> tree, ConditionViolationChecker<T> checker) {
    tree.accept(new ConditionViolationsCollector<>(context, checker), null);
  }

  @Override
  public Void visitBranch(final BranchElimTreeNode<T> branchNode, List<Expression> expressions) {
    if (expressions == null) {
      expressions = new ArrayList<>(branchNode.getIndex() + 1);
      for (int i = 0; i <= branchNode.getIndex(); i++) {
        expressions.add(Index(i));
      }
    }

    List<Expression> parameters = new ArrayList<>();
    Expression type = myContext.get(myContext.size() - 1 - branchNode.getIndex()).getType().liftIndex(0, branchNode.getIndex());
    DataDefinition dataType = ((DataCallExpression) type.normalize(NormalizeVisitor.Mode.NF, myContext).getFunction(parameters)).getDefinition();
    Collections.reverse(parameters);
    for (ConCallExpression conCall : dataType.getConstructors(parameters, myContext)) {
      ElimTreeNode<T> child = branchNode.getChild(conCall.getDefinition());
      if (child != null) {
        try (ConCallContextExpander expander = new ConCallContextExpander(branchNode.getIndex(), conCall, myContext)) {
          child.accept(this, expander.substIn(expressions));
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


          SubstituteExpander.substituteExpand(myContext, subst, branchNode, newExpressions, new SubstituteExpander.SubstituteExpansionProcessor<T>() {
            @Override
            public void process(List<Expression> expressions1, List<Binding> context, final T lhsValue) {
              List<Expression> newSubst = new ArrayList<>(expressions1.subList(expressions1.size() - 1 - branchNode.getIndex(), expressions1.size()));
              expressions1.subList(expressions1.size() - 1 - branchNode.getIndex(), expressions1.size()).clear();
              SubstituteExpander.substituteExpand(context, newSubst, branchNode, expressions1, new SubstituteExpander.SubstituteExpansionProcessor<T>() {
                @Override
                public void process(List<Expression> expressions2, List<Binding> context, T rhsValue) {
                  myChecker.check(context, lhsValue, new ArrayList<>(expressions2.subList(0, expressions2.size() / 2)), rhsValue, new ArrayList<>(expressions2.subList(expressions2.size() / 2, expressions2.size())));
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
  public Void visitLeaf(LeafElimTreeNode<T> leafNode, List<Expression> expressions) {
    return null;
  }
}
