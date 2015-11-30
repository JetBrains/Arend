package com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor;

import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.IndexExpression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.BranchElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Apps;

public class SubstituteExpander<T> implements ElimTreeNodeVisitor<T, List<SubstituteExpander.SubstituteExpansionResult<T>>, SubstituteExpander.SubstituteExpansionParams> {
  public static class SubstituteExpansionParams {
    public final List<Expression> subst;
    public final Expression expression;

    public SubstituteExpansionParams(List<Expression> subst, Expression expression) {
      this.subst = subst;
      this.expression = expression;
    }
  }

 public static class SubstituteExpansionResult<T> {
    public final List<Expression> subst;
    public final Expression expression;
    public final List<Binding> context;
    public final T value;

    public SubstituteExpansionResult(List<Expression> subst, Expression expression, List<Binding> context, T value) {
      this.subst = subst;
      this.expression = expression;
      this.context = context;
      this.value = value;
    }
  }

  private final List<Binding> myContext;

  public SubstituteExpander(List<Binding> myContext) {
    this.myContext = myContext;
  }

  @Override
  public List<SubstituteExpansionResult<T>> visitBranch(BranchElimTreeNode<T> branchNode, SubstituteExpansionParams params) {
    List<Expression> arguments = new ArrayList<>();
    Expression func = params.subst.get(branchNode.getIndex()).getFunction(arguments);
    List<SubstituteExpansionResult<T>> results = new ArrayList<>();

    if (func instanceof ConCallExpression) {
      ElimTreeNode<T> child = branchNode.getChild(((ConCallExpression) func).getDefinition());
      if (child != null) {
        List<Expression> constructorArgs = new ArrayList<>();
        ConCallExpression conCall = (ConCallExpression) params.subst.get(branchNode.getIndex()).getFunction(constructorArgs);
        List<Expression> newSubst = new ArrayList<>(params.subst.subList(0, branchNode.getIndex()));
        newSubst.addAll(constructorArgs);
        newSubst.addAll(params.subst.subList(branchNode.getIndex() + 1, params.subst.size()));
        for (SubstituteExpansionResult<T> nestedResult : child.accept(this, new SubstituteExpansionParams(newSubst, params.expression))) {
          List<Expression> newArguments = new ArrayList<>(nestedResult.subst.subList(branchNode.getIndex(), branchNode.getIndex() + constructorArgs.size()));
          Collections.reverse(newArguments);
          nestedResult.subst.subList(branchNode.getIndex(), branchNode.getIndex() + constructorArgs.size()).clear();
          nestedResult.subst.add(branchNode.getIndex(), Apps(
              conCall.liftIndex(0, nestedResult.context.size() - myContext.size()), newArguments.toArray(new Expression[newArguments.size()])
          ));
          results.add(nestedResult);
        }
      }
    } else if (func instanceof IndexExpression) {
      final int varIndex = ((IndexExpression) func).getIndex();
      List<Expression> parameters = new ArrayList<>();
      Expression ftype = myContext.get(myContext.size() - 1 - varIndex).getType().liftIndex(0, varIndex).
          normalize(NormalizeVisitor.Mode.NF, myContext).getFunction(parameters);
      for (ConCallExpression conCall : ((DataCallExpression) ftype).getDefinition().getConstructors(parameters, myContext)) {
        try (ConCallContextExpander expander = new ConCallContextExpander(varIndex, conCall, myContext)) {
          List<Expression> newSubst = new ArrayList<>();
          for (Expression expr : params.subst) {
            newSubst.add(expander.substIn(expr));
          }
          results.addAll(visitBranch(branchNode, new SubstituteExpansionParams(newSubst, expander.substIn(params.expression))));
        }
      }
    } else {
      throw new IllegalStateException();
    }

    return results;
  }

  @Override
  public List<SubstituteExpansionResult<T>> visitLeaf(LeafElimTreeNode<T> leafNode, SubstituteExpansionParams params) {
    return Collections.singletonList(new SubstituteExpansionResult<>(
        params.subst, params.expression, new ArrayList<>(myContext), leafNode.getValue()
    ));
  }

 }
