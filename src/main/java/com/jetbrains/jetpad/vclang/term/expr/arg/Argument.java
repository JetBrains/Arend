package com.jetbrains.jetpad.vclang.term.expr.arg;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.visitor.ExpressionVisitor;

public abstract class Argument extends Expression implements Abstract.Argument {
  private final boolean myExplicit;

  public Argument(boolean explicit) {
    myExplicit = explicit;
  }

  @Override
  public boolean getExplicit() {
    return myExplicit;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    throw new IllegalStateException();
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    throw new IllegalStateException();
  }
}
