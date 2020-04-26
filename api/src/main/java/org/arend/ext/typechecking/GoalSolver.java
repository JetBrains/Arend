package org.arend.ext.typechecking;

import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.concrete.expr.ConcreteGoalExpression;
import org.arend.ext.core.expr.CoreExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A goal solver provides two functions {@link #fillGoal} and {@link #trySolve}.
 * The former is usually used to typecheck the expression in the goal and apply some simple transformations to it.
 * The latter can be used to do more complicated computations, proof search, and interaction with the user.
 */
public interface GoalSolver {
  class FillGoalResult {
    public final @Nullable ConcreteExpression concreteExpression;
    public final @Nullable TypedExpression typedExpression;

    public FillGoalResult(@Nullable ConcreteExpression concreteExpression, @Nullable TypedExpression typedExpression) {
      this.concreteExpression = concreteExpression;
      this.typedExpression = typedExpression;
    }
  }

  /**
   * Invoked immediately on the goal.
   * The result will be used to fill the goal.
   */
  default @NotNull FillGoalResult fillGoal(@NotNull ExpressionTypechecker typechecker, @NotNull ConcreteGoalExpression goalExpression, @Nullable CoreExpression expectedType) {
    ConcreteExpression expr = goalExpression.getExpression();
    return new FillGoalResult(expr, expr == null ? null : typechecker.typecheck(expr, expectedType));
  }

  /**
   * @return true if {@link #trySolve} should be invoked, false otherwise.
   */
  default boolean willTrySolve(@NotNull ConcreteGoalExpression goalExpression, @Nullable CoreExpression expectedType) {
    return false;
  }

  /**
   * Invoked on a user's request.
   * This method is invoked only if {@link #fillGoal} fails.
   */
  default @Nullable ConcreteExpression trySolve(@NotNull ExpressionTypechecker typechecker, @NotNull ConcreteGoalExpression goalExpression, @Nullable CoreExpression expectedType) {
    return null;
  }
}
