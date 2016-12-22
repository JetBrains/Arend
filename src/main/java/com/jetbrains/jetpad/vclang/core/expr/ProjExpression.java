package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;

public class ProjExpression extends Expression {
  private final Expression myExpression;
  private final int myField;

  public ProjExpression(Expression expression, int field) {
    myExpression = expression;
    myField = field;
  }

  public Expression getExpression() {
    return myExpression;
  }

  public int getField() {
    return myField;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitProj(this, params);
  }

  @Override
  public ProjExpression toProj() {
    return this;
  }
}
