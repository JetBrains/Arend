package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

public class NewExpression extends Expression {
  private final Expression myExpression;

  public NewExpression(Expression expression) {
    myExpression = expression;
  }

  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public Expression getType() {
    return myExpression;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitNew(this, params);
  }

  @Override
  public NewExpression toNew() {
    return this;
  }
}
