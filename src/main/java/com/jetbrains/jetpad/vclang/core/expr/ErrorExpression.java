package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalError;

public class ErrorExpression extends Expression {
  private final Expression myExpression;
  private final LocalError myError;

  public ErrorExpression(Expression expression, LocalError error) {
    myExpression = expression;
    myError = error;
  }

  public Expression getExpression() {
    return myExpression;
  }

  public LocalError getError() {
    return myError;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitError(this, params);
  }

  @Override
  public boolean isWHNF() {
    return true;
  }

  @Override
  public Expression getStuckExpression() {
    return this;
  }
}
