package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

public class DataCallExpression extends DefCallExpression {
  public DataCallExpression(DataDefinition definition) {
    super(definition);
  }

  @Override
  public DataDefinition getDefinition() {
    return (DataDefinition) super.getDefinition();
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitDataCall(this);
  }
}
