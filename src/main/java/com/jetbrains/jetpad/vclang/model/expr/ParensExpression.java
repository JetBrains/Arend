package com.jetbrains.jetpad.vclang.model.expr;

public abstract class ParensExpression extends Expression {
  public final boolean parens;

  public ParensExpression(boolean parens) {
    this.parens = parens;
  }
}
