package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

public class ErrorExpression extends Expression {
  private final Expression myExpression;
  private final LocalTypeCheckingError myError;

  public ErrorExpression(Expression expression, LocalTypeCheckingError error) {
    myExpression = expression;
    myError = error;
  }

  public Expression getExpression() {
    return myExpression;
  }

  public LocalTypeCheckingError getError() {
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
