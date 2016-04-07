package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

public class FieldCallExpression extends DefCallExpression {
  public FieldCallExpression(ClassField definition) {
    super(definition);
  }

  @Override
  public Expression applyThis(Expression thisExpr) {
    return ExpressionFactory.Apps(this, thisExpr);
  }

  @Override
  public ClassField getDefinition() {
    return (ClassField) super.getDefinition();
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitFieldCall(this, params);
  }

  @Override
  public FieldCallExpression toFieldCall() {
    return this;
  }
}
