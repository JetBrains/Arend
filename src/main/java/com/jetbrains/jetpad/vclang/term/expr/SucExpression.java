package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.visitor.ExpressionVisitor;

public class SucExpression extends Expression implements Abstract.SucExpression {
  @Override
  public boolean equals(Object o) {
    return o == this || o instanceof SucExpression;
  }

  @Override
  public String toString() {
    return "S";
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitSuc(this);
  }

  @Override
  public <T> T accept(AbstractExpressionVisitor<? extends T> visitor) {
    return visitor.visitSuc(this);
  }
}
