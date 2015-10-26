package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

public class FieldCallExpression extends DefCallExpression {
  public FieldCallExpression(ClassField definition) {
    super(definition);
  }

  @Override
  public ClassField getDefinition() {
    return (ClassField) super.getDefinition();
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitFieldCall(this);
  }
}
