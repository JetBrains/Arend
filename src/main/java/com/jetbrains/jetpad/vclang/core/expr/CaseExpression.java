package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.elimtree.ElimTree;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;

import java.util.List;

public class CaseExpression extends Expression {
  private final DependentLink myParameters;
  private final Expression myResultType;
  private final ElimTree myElimTree;
  private final List<Expression> myArguments;

  public CaseExpression(DependentLink parameters, Expression resultType, ElimTree elimTree, List<Expression> arguments) {
    myParameters = parameters;
    myElimTree = elimTree;
    myResultType = resultType;
    myArguments = arguments;
  }

  public DependentLink getParameters() {
    return myParameters;
  }

  public Expression getResultType() {
    return myResultType;
  }

  public ElimTree getElimTree() {
    return myElimTree;
  }

  public List<Expression> getArguments() {
    return myArguments;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitCase(this, params);
  }
}
