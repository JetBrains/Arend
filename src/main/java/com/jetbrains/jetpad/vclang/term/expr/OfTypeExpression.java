package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.List;

public class OfTypeExpression extends Expression {
  private final Expression myExpression;
  private final Expression myType;

  public OfTypeExpression(Expression expression, Expression type) {
    myExpression = expression;
    myType = type;
  }

  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public Expression getFunction() {
    return myExpression.getFunction();
  }

  @Override
  public List<? extends Expression> getArguments() {
    return myExpression.getArguments();
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitOfType(this, params);
  }

  @Override
  public Expression getType() {
    return myType;
  }
}
