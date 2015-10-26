package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

public class FunCallExpression extends DefCallExpression {
  public FunCallExpression(FunctionDefinition definition) {
    super(definition);
  }

  @Override
  public FunctionDefinition getDefinition() {
    return (FunctionDefinition) super.getDefinition();
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitFunCall(this);
  }
}
