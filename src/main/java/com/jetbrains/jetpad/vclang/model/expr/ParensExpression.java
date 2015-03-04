package com.jetbrains.jetpad.vclang.model.expr;

import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;

public class ParensExpression extends Expression {
  public final Property<Expression> expr = new ValueProperty<>();

  public ParensExpression(Expression expr) {
    this.expr.set(expr);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return expr.get().accept(visitor, params);
  }
}
