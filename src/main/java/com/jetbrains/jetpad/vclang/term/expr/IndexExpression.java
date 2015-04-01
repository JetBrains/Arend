package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.visitor.ExpressionVisitor;

public class IndexExpression extends Expression implements Abstract.IndexExpression {
  private final int index;

  public IndexExpression(int index) {
    this.index = index;
  }

  @Override
  public int getIndex() {
    return index;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitIndex(this);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitIndex(this, params);
  }
}
