package org.arend.core.expr;

import org.arend.typechecking.error.local.GoalError;

public class GoalErrorExpression extends ErrorExpression {
  public final GoalError goalError;

  public GoalErrorExpression(GoalError goalError) {
    super(goalError);
    this.goalError = goalError;
  }

  public GoalErrorExpression(Expression expression, GoalError goalError) {
    super(expression, goalError);
    this.goalError = goalError;
  }

  @Override
  public GoalErrorExpression replaceExpression(Expression expr) {
    return new GoalErrorExpression(expr, goalError);
  }
}
