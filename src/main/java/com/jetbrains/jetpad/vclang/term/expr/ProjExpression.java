package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

public class ProjExpression extends Expression implements Abstract.ProjExpression {
  private final Expression myExpression;
  private final int myField;

  public ProjExpression(Expression expression, int field) {
    myExpression = expression;
    myField = field;
  }

  @Override
  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public int getField() {
    return myField;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitProj(this);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitProj(this, params);
  }
}
