package org.arend.core.expr;

import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.util.Decision;

public class ProjExpression extends Expression {
  private final Expression myExpression;
  private final int myField;

  private ProjExpression(Expression expression, int field) {
    myExpression = expression;
    myField = field;
  }

  public static Expression make(Expression expression, int field) {
    TupleExpression tuple = expression.cast(TupleExpression.class);
    return tuple != null ? tuple.getFields().get(field) : new ProjExpression(expression, field);
  }

  public Expression getExpression() {
    return myExpression;
  }

  public int getField() {
    return myField;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitProj(this, params);
  }

  @Override
  public Decision isWHNF() {
    return myExpression.isInstance(TupleExpression.class) ? Decision.NO : myExpression.isWHNF();
  }

  @Override
  public Expression getStuckExpression() {
    return myExpression.getStuckExpression();
  }
}
