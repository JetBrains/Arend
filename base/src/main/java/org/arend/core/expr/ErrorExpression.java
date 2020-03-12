package org.arend.core.expr;

import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.ext.core.expr.CoreErrorExpression;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.ext.error.GeneralError;
import org.arend.ext.error.LocalError;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;

public class ErrorExpression extends Expression implements CoreErrorExpression {
  private final Expression myExpression;
  private final boolean myGoal;

  public ErrorExpression(Expression expression, LocalError error) {
    myExpression = expression;
    myGoal = error != null && error.level == GeneralError.Level.GOAL;
  }

  public ErrorExpression(LocalError error) {
    myExpression = null;
    myGoal = error != null && error.level == GeneralError.Level.GOAL;
  }

  public ErrorExpression(Expression expression, boolean isGoal) {
    myExpression = expression;
    myGoal = isGoal;
  }

  public ErrorExpression(Expression expression) {
    myExpression = expression;
    myGoal = false;
  }

  public ErrorExpression() {
    myExpression = null;
    myGoal = false;
  }

  public Expression getExpression() {
    return myExpression;
  }

  public ErrorExpression replaceExpression(Expression expr) {
    return new ErrorExpression(expr, myGoal);
  }

  public boolean isGoal() {
    return myGoal;
  }

  public boolean isError() {
    return !myGoal;
  }

  @Override
  public boolean canBeConstructor() {
    return false;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitError(this, params);
  }

  @Override
  public <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2) {
    return visitor.visitError(this, param1, param2);
  }

  @Override
  public <P, R> R accept(@NotNull CoreExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitError(this, params);
  }

  @Override
  public Decision isWHNF() {
    return Decision.YES;
  }

  @Override
  public Expression getStuckExpression() {
    return this;
  }
}
