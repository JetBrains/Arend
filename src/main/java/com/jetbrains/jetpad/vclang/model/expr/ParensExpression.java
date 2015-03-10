package com.jetbrains.jetpad.vclang.model.expr;

import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;

public class ParensExpression extends Expression {
  public final Property<Expression> expression = new ValueProperty<>();

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return expression.get().accept(visitor, params);
  }

  public static Expression parens(boolean p, Expression expr) {
    if (p) {
      ParensExpression pexpr = new ParensExpression();
      pexpr.expression.set(expr);
      return pexpr;
    } else {
      return expr;
    }
  }
}
