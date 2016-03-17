package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

public class DataCallExpression extends DefCallExpression {
  public DataCallExpression(DataDefinition definition) {
    super(definition);
  }

  @Override
  public Expression applyThis(Expression thisExpr) {
    return ExpressionFactory.Apps(this, thisExpr);
  }

  @Override
  public DataDefinition getDefinition() {
    return (DataDefinition) super.getDefinition();
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitDataCall(this, params);
  }
}
