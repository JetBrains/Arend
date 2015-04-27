package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

public class SucExpression extends Expression implements Abstract.SucExpression {
  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitSuc(this);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitSuc(this, params);
  }
}
