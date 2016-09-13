package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

public class NewExpression extends Expression {
  private final ClassCallExpression myExpression;

  public NewExpression(ClassCallExpression expression) {
    myExpression = expression;
  }

  public ClassCallExpression getExpression() {
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
