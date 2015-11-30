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

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Apps;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Index;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.splitArguments;
import static com.jetbrains.jetpad.vclang.term.pattern.PatternExpansion.expandPattern;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.expandPatternSubstitute;

public class ConditionViolationsCollector<T> implements ElimTreeNodeVisitor<T, List<ConditionViolationsCollector.ConditionCheckPair<T>>, Void>  {

  public static class ConditionCheckPair<T> {
    public final List<Binding> ctx;
    public final T v1;
    public final List<Expression> subst1;
    public final T v2;
    public final List<Expression> subst2;

    public ConditionCheckPair(List<Binding> ctx, T v1, List<Expression> subst1, T v2, List<Expression> subst2) {
      this.ctx = ctx;
      this.v1 = v1;
      this.subst1 = subst1;
      this.v2 = v2;
      this.subst2 = subst2;
    }
  }

  private final List<Binding> myContext;

  public ConditionViolationsCollector(List<Binding> context) {
    this.myContext = context;
  }

  @Override
  public List<ConditionCheckPair<T>> visitBranch(BranchElimTreeNode<T> branchNode, Void params) {
    List<Expression> parameters = new ArrayList<>();
    Expression type = myContext.get(myContext.size() - 1 - branchNode.getIndex()).getType().liftIndex(0, branchNode.getIndex());
    DataDefinition dataType = ((DataCallExpression) type.normalize(NormalizeVisitor.Mode.NF, myContext).getFunction(parameters)).getDefinition();
    List<ConditionCheckPair<T>> result = new ArrayList<>();
    for (ConCallExpression conCall : dataType.getConstructors(parameters, myContext)) {
      ElimTreeNode<T> child = branchNode.getChild(conCall.getDefinition());
      if (child != null) {
        try (ConCallContextExpander ignore = new ConCallContextExpander(branchNode.getIndex(), conCall, myContext)) {
          int numArgs = splitArguments(conCall.getDefinition().getArguments()).size();
          for (ConditionCheckPair<T> pair : child.accept(this, null)) {
            fixSubst(branchNode.getIndex(), conCall, numArgs, pair.ctx, pair.subst1);
            fixSubst(branchNode.getIndex(), conCall, numArgs, pair.ctx, pair.subst2);
            result.add(pair);
          }
        }
      }
    }

    if (dataType.getConditions() != null) {
      for (Condition condition : dataType.getConditions()) {
        if (branchNode.getChild(condition.getConstructor()) != null) {
          List<Binding> tail = new ArrayList<>(myContext.subList(myContext.size() - 1 - branchNode.getIndex(), myContext.size()));
          myContext.subList(myContext.size() - 1 - branchNode.getIndex(), myContext.size()).clear();
          Pattern pattern = new ConstructorPattern(condition.getConstructor(), condition.getPatterns(), true);
          PatternExpansion.Result expansionResult = expandPattern(pattern, type.liftIndex(0, -branchNode.getIndex()), myContext);
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
          subst.add(expansionResult.expression.getExpression().liftIndex(0, tail.size() - 1));

          for (SubstituteExpander.SubstituteExpansionResult<T> nestedResult : branchNode.accept(new SubstituteExpander<T>(myContext),
              new SubstituteExpander.SubstituteExpansionParams(subst, condition.getTerm().liftIndex(0, branchNode.getIndex())))) {
            Expression newExpression = nestedResult.subst.get(nestedResult.subst.size() - 1);
            nestedResult.subst.remove(nestedResult.subst.size() - 1);
            nestedResult.subst.add(nestedResult.expression);
            for (SubstituteExpander.SubstituteExpansionResult<T> veryNestedResult : branchNode.accept(new SubstituteExpander<T>(nestedResult.context),
                new SubstituteExpander.SubstituteExpansionParams(nestedResult.subst, newExpression))) {
              List<Expression> subst1 = new ArrayList<>(veryNestedResult.subst.subList(0, veryNestedResult.subst.size() - 1));
              subst1.add(veryNestedResult.expression);
              result.add(new ConditionCheckPair<>(
                veryNestedResult.context, nestedResult.value, subst1, veryNestedResult.value, veryNestedResult.subst
              ));
            }
          }
        }
      }
    }

    return result;
  }

  private void fixSubst(int index, ConCallExpression conCall, int numArgs, List<Binding> newContext, List<Expression> subst) {
    while (subst.size() < index + numArgs) {
      subst.add(Index(newContext.size() - 1 - (myContext.size() - 1 - subst.size())));
    }

    List<Expression> args1 = new ArrayList<>(subst.subList(index, index + numArgs));
    subst.subList(index, index + numArgs).clear();
    subst.add(index, Apps(conCall.liftIndex(0, newContext.size() - myContext.size()), args1.toArray(new Expression[args1.size()])));
  }

  @Override
  public List<ConditionCheckPair<T>> visitLeaf(LeafElimTreeNode<T> leafNode, Void params) {
    return Collections.emptyList();
  }
}
