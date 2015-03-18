package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.typechecking.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.visitor.ExpressionVisitor;

public class ErrorExpression extends Expression {
  private final Expression myExpr;
  private final TypeCheckingError myError;

  public ErrorExpression(Expression expr, TypeCheckingError error) {
    myExpr = expr;
    myError = error;
  }

  public Expression expression() {
    return myExpr;
  }

  public TypeCheckingError error() {
    return myError;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitError(this);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return myExpr.accept(visitor, params);
  }
}
