package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

public class ZeroExpression extends Expression implements Abstract.ZeroExpression {
  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitZero(this);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitZero(this, params);
  }
}
