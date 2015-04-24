package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.visitor.ExpressionVisitor;

public class NelimExpression extends Expression implements Abstract.NelimExpression {
  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitNelim(this);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitNelim(this, params);
  }
}
