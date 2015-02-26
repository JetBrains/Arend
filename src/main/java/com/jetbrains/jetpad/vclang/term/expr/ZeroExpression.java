package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.visitor.ExpressionVisitor;

public class ZeroExpression extends Expression implements Abstract.ZeroExpression {
  @Override
  public boolean equals(Object o) {
    return o == this || o instanceof ZeroExpression;
  }

  @Override
  public String toString() {
    return "0";
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitZero(this);
  }

  @Override
  public <T> T accept(AbstractExpressionVisitor<? extends T> visitor) {
    return visitor.visitZero(this);
  }
}
