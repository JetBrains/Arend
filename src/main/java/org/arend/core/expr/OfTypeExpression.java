package org.arend.core.expr;

import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.util.Decision;

public class OfTypeExpression extends Expression {
  private final Expression myExpression;
  private final Expression myType;

  public OfTypeExpression(Expression expression, Expression type) {
    myExpression = expression;
    myType = type;
  }

  public static Expression make(Expression expression, Expression actualType, Expression expectedType) {
    Expression expectedType1 = expectedType.getUnderlyingExpression();
    if (!(expectedType1 instanceof PiExpression || expectedType1 instanceof SigmaExpression || expectedType1 instanceof ClassCallExpression)) {
      return expression;
    }
    Expression actualType1 = actualType.getUnderlyingExpression();
    if (actualType1 instanceof PiExpression || actualType1 instanceof SigmaExpression || actualType1 instanceof ClassCallExpression) {
      return expression;
    }

    while (expression instanceof OfTypeExpression) {
      expression = ((OfTypeExpression) expression).myExpression;
    }
    return new OfTypeExpression(expression, expectedType);
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
  public Expression getUnderlyingExpression() {
    return myExpression.getUnderlyingExpression();
  }

  @Override
  public boolean isInstance(Class clazz) {
    return clazz.isInstance(this) || myExpression.isInstance(clazz);
  }

  @Override
  public <T extends Expression> T cast(Class<T> clazz) {
    return clazz.isInstance(this) ? clazz.cast(this) : myExpression.cast(clazz);
  }

  @Override
  public Decision isWHNF() {
    return myExpression.isWHNF();
  }

  @Override
  public Expression getStuckExpression() {
    return myExpression.getStuckExpression();
  }
}
