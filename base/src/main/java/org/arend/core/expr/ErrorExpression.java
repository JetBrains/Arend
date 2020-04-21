package org.arend.core.expr;

import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.ext.core.expr.CoreErrorExpression;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.ext.error.GeneralError;
import org.arend.ext.error.LocalError;
import org.arend.typechecking.error.local.GoalError;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;

public class ErrorExpression extends Expression implements CoreErrorExpression {
  private final Expression myExpression;
  private final boolean myGoal;
  private final boolean myUseExpression;

  public ErrorExpression(Expression expression, LocalError error) {
    myExpression = expression;
    myGoal = error != null && error.level == GeneralError.Level.GOAL;
    myUseExpression = expression != null && error instanceof GoalError && ((GoalError) error).errors.isEmpty();
  }

  public ErrorExpression(LocalError error) {
    this(null, error);
  }

  public ErrorExpression(Expression expression, boolean isGoal, boolean useExpression) {
    myExpression = expression;
    myGoal = isGoal;
    myUseExpression = useExpression && expression != null;
  }

  public ErrorExpression(Expression expression) {
    myExpression = expression;
    myGoal = false;
    myUseExpression = false;
  }

  public ErrorExpression() {
    myExpression = null;
    myGoal = false;
    myUseExpression = false;
  }

  public Expression getExpression() {
    return myExpression;
  }

  public ErrorExpression replaceExpression(Expression expr) {
    return new ErrorExpression(expr, myGoal, myUseExpression);
  }

  public boolean isGoal() {
    return myGoal;
  }

  public boolean isError() {
    return !myGoal;
  }

  public boolean useExpression() {
    return myUseExpression;
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
  public @NotNull Expression getUnderlyingExpression() {
    return myUseExpression ? myExpression.getUnderlyingExpression() : this;
  }

  @Override
  public <T extends Expression> boolean isInstance(Class<T> clazz) {
    return myUseExpression && myExpression.isInstance(clazz) || clazz.isInstance(this);
  }

  @Override
  public <T extends Expression> T cast(Class<T> clazz) {
    return clazz.isInstance(this) ? clazz.cast(this) : myUseExpression ? myExpression.cast(clazz) : null;
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
