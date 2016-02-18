package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;

public class ErrorExpression extends Expression {
  private final Expression myExpr;
  private final TypeCheckingError myError;

  public ErrorExpression(Expression expr, TypeCheckingError error) {
    myExpr = expr;
    myError = error;
  }

  public Expression getExpr() {
    return myExpr;
  }

  public TypeCheckingError getError() {
    return myError;
  }

  @Override
  public Expression getType() {
    return null;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitError(this, params);
  }
}
