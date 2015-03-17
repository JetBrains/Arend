package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.visitor.ExpressionVisitor;

public class ErrorExpression extends Expression {
  private final Expression myExpr;
  private final String myMessage;

  public ErrorExpression(Expression expr, String message) {
    myExpr = expr;
    myMessage = message;
  }

  public Expression expression() {
    return myExpr;
  }

  public String message() {
    return myMessage;
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
