package com.jetbrains.jetpad.vclang.term.expr;

public class InferHoleExpression extends HoleExpression implements Abstract.InferHoleExpression {
  public InferHoleExpression() {
    super(null);
  }

  @Override
  public InferHoleExpression getInstance(Expression expr) {
    return this;
  }
}
