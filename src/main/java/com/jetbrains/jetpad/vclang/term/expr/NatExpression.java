package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.visitor.ExpressionVisitor;

public class NatExpression extends Expression implements Abstract.NatExpression {
  @Override
  public boolean equals(Object o) {
    return o == this || o instanceof NatExpression;
  }

  @Override
  public String toString() {
    return "N";
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitNat(this);
  }

  @Override
  public <T> T accept(AbstractExpressionVisitor<? extends T> visitor) {
    return visitor.visitNat(this);
  }
}
