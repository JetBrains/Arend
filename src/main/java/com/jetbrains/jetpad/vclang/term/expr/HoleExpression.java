package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.visitor.ExpressionVisitor;

public abstract class HoleExpression extends Expression implements Abstract.HoleExpression {
  private final Expression myExpr;

  public HoleExpression(Expression expr) {
    myExpr = expr;
  }

  public Expression expression() {
    return myExpr;
  }

  public abstract HoleExpression getInstance(Expression expr);

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitHole(this);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitHole(this, params);
  }
}
