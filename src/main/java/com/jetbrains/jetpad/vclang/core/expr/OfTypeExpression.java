package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;

public class OfTypeExpression extends Expression {
  private final Expression myExpression;
  private final Expression myType;

  public OfTypeExpression(Expression expression, Expression type) {
    myExpression = expression;
    myType = type;
  }

  public static Expression make(Expression expression, Expression actualType, Expression expectedType) {
    if ((expectedType.isInstance(PiExpression.class) || expectedType.isInstance(SigmaExpression.class) || expectedType.isInstance(ClassCallExpression.class)) &&
        !(actualType.isInstance(PiExpression.class) || actualType.isInstance(SigmaExpression.class) || actualType.isInstance(ClassCallExpression.class))) {
      while (expression.isInstance(OfTypeExpression.class)) {
        expression = expression.cast(OfTypeExpression.class).myExpression;
      }
      return new OfTypeExpression(expression, expectedType);
    } else {
      return expression;
    }
  }

  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitOfType(this, params);
  }

  public Expression getTypeOf() {
    return myType;
  }

  @Override
  public <T extends Expression> T cast(Class<T> clazz) {
    return clazz.isInstance(this) ? clazz.cast(this) : myExpression.cast(clazz);
  }

  @Override
  public boolean isInstance(Class clazz) {
    return clazz.isInstance(myExpression) || clazz.isInstance(this);
  }

  @Override
  public boolean isWHNF() {
    return myExpression.isWHNF();
  }

  @Override
  public Expression getStuckExpression() {
    return myExpression.getStuckExpression();
  }
}
