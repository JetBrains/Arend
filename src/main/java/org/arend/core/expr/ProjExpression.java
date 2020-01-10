package org.arend.core.expr;

import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.ext.core.expr.CoreProjExpression;
import org.arend.util.Decision;

import javax.annotation.Nonnull;

public class ProjExpression extends Expression implements CoreProjExpression {
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

  @Nonnull
  @Override
  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public int getField() {
    return myField;
  }

  @Override
  public boolean canBeConstructor() {
    return myExpression.canBeConstructor();
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitProj(this, params);
  }

  @Override
  public <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2) {
    return visitor.visitProj(this, param1, param2);
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
