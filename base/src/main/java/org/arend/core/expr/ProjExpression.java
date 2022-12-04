package org.arend.core.expr;

import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.ext.core.expr.CoreProjExpression;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;

public class ProjExpression extends Expression implements CoreProjExpression {
  private final Expression myExpression;
  private final int myField;
  private final boolean myProperty;

  private ProjExpression(Expression expression, int field, boolean isProperty) {
    myExpression = expression;
    myField = field;
    myProperty = isProperty;
  }

  public static Expression make(Expression expression, int field, boolean isProperty) {
    TupleExpression tuple = isProperty ? null : expression.cast(TupleExpression.class);
    return tuple != null ? tuple.getFields().get(field) : new ProjExpression(expression, field, isProperty);
  }

  @NotNull
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
  public <P, R> R accept(@NotNull CoreExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitProj(this, params);
  }

  @Override
  public boolean isBoxed() {
    return myProperty;
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
