package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

public class ClassCallExpression extends DefCallExpression {
  public ClassCallExpression(ClassDefinition definition) {
    super(definition);
  }

  @Override
  public ClassDefinition getDefinition() {
    return (ClassDefinition) super.getDefinition();
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitClassCall(this);
  }
}
