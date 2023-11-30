package org.arend.core.expr;

import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.ext.core.expr.CoreBoxExpression;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;

public class BoxExpression extends Expression implements CoreBoxExpression {
  private final Expression myExpression;
  private final Expression myType;

  private BoxExpression(Expression expression, Expression type) {
    myExpression = expression;
    myType = type;
  }

  public static BoxExpression make(Expression expression, Expression type) {
    return expression instanceof BoxExpression ? (BoxExpression) expression : new BoxExpression(expression, type);
  }

  @Override
  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public Expression getType() {
    return myType;
  }

  @Override
  public @NotNull Expression computeType() {
    return myType;
  }

  @Override
  public <P, R> R accept(@NotNull CoreExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitBox(this, params);
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitBox(this, params);
  }

  @Override
  public <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2) {
    return visitor.visitBox(this, param1, param2);
  }

  @Override
  public Decision isWHNF() {
    return Decision.YES;
  }

  @Override
  public Expression getStuckExpression() {
    return this;
  }

  @Override
  public boolean canBeConstructor() {
    return false;
  }

  @Override
  public boolean isBoxed() {
    return true;
  }
}
