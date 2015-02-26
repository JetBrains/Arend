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
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof IndexExpression)) return false;
    IndexExpression other = (IndexExpression)o;
    return index == other.index;
  }

  @Override
  public String toString() {
    return "<" + index + ">";
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitIndex(this);
  }

  @Override
  public <T> T accept(AbstractExpressionVisitor<? extends T> visitor) {
    return visitor.visitIndex(this);
  }
}
