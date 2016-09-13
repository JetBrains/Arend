package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

public class FieldCallExpression extends DefCallExpression {
  private Expression myExpression;

  public FieldCallExpression(ClassField definition, Expression expression) {
    super(definition);
    myExpression = expression;
  }

  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public Expression applyThis(Expression thisExpr) {
    assert myExpression == null;
    myExpression = thisExpr;
    return this;
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
