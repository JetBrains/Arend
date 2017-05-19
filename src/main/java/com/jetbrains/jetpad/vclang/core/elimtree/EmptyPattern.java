package com.jetbrains.jetpad.vclang.core.elimtree;

import com.jetbrains.jetpad.vclang.core.expr.Expression;

public class EmptyPattern implements Pattern {
  public final static EmptyPattern INSTANCE = new EmptyPattern();

  private EmptyPattern() {}

  @Override
  public Expression getExpression() {
    return null;
  }
}
