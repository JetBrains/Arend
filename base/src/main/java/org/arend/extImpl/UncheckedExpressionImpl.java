package org.arend.extImpl;

import org.arend.core.expr.Expression;
import org.arend.ext.core.expr.UncheckedExpression;

public class UncheckedExpressionImpl implements UncheckedExpression {
  private final Expression myExpression;

  public UncheckedExpressionImpl(Expression expression) {
    myExpression = expression;
  }

  public static Expression extract(UncheckedExpression expr) {
    if (expr instanceof Expression) {
      return (Expression) expr;
    }
    if (expr instanceof UncheckedExpressionImpl) {
      return ((UncheckedExpressionImpl) expr).myExpression;
    }
    throw new IllegalArgumentException();
  }
}
