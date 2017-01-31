package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

public class ErrorExpression extends Expression {
  private final Expression myExpr;
  private final LocalTypeCheckingError myError;

  public ErrorExpression(Expression expr, LocalTypeCheckingError error) {
    myExpr = expr;
    myError = error;
  }

  public Expression getExpr() {
    return myExpr;
  }

  public LocalTypeCheckingError getError() {
    return myError;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitError(this, params);
  }

  @Override
  public ErrorExpression toError() {
    return this;
  }

  @Override
  public Expression getStuckExpression() {
    return this;
  }
}
