package org.arend.core.expr;

import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.prelude.Prelude;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;

public class OfTypeExpression extends Expression {
  private final Expression myExpression;
  private final Expression myType;

  public OfTypeExpression(Expression expression, Expression type) {
    myExpression = expression;
    myType = type;
  }

  public static Expression make(Expression expression, Expression actualType, Expression expectedType) {
    Expression expectedType1 = expectedType.getUnderlyingExpression();
    if (expression instanceof PathExpression && expectedType1 instanceof DataCallExpression && ((DataCallExpression) expectedType1).getDefinition() == Prelude.PATH) {
      return new OfTypeExpression(expression, expectedType);
    }

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
  public boolean canBeConstructor() {
    return myExpression.canBeConstructor();
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitOfType(this, params);
  }

  @Override
  public <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2) {
    return visitor.visitOfType(this, param1, param2);
  }

  @Override
  public <P, R> R accept(@NotNull CoreExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return myExpression.accept(visitor, params);
  }

  public Expression getTypeOf() {
    return myType;
  }

  @NotNull
  @Override
  public Expression getUnderlyingExpression() {
    return myExpression.getUnderlyingExpression();
  }

  @Override
  public <T extends Expression> boolean isInstance(Class<T> clazz) {
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
