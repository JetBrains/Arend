package org.arend.ext.typechecking;

import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.concrete.expr.ConcreteGoalExpression;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.ui.ArendUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A goal solver provides two functions {@link #checkGoal} and {@link #trySolve}.
 * The former is usually used to typecheck the expression in the goal and apply some simple transformations to it.
 * The latter can be used to do more complicated computations, proof search, and interaction with the user.
 */
public interface GoalSolver {
  class CheckGoalResult {
    public final @Nullable ConcreteExpression concreteExpression;
    public final @Nullable TypedExpression typedExpression;

    public CheckGoalResult(@Nullable ConcreteExpression concreteExpression, @Nullable TypedExpression typedExpression) {
      this.concreteExpression = concreteExpression;
      this.typedExpression = typedExpression;
    }
  }

  /**
   * Invoked immediately on the goal.
   */
  default @NotNull CheckGoalResult checkGoal(@NotNull ExpressionTypechecker typechecker, @NotNull ConcreteGoalExpression goalExpression, @Nullable CoreExpression expectedType) {
    ConcreteExpression expr = goalExpression.getExpression();
    return new CheckGoalResult(expr, expr == null ? null : typechecker.typecheck(expr, expectedType));
  }

  /**
   * @return true if {@link #trySolve} should be invoked, false otherwise.
   */
  default boolean willTrySolve(@NotNull ConcreteGoalExpression goalExpression, @Nullable CoreExpression expectedType) {
    return false;
  }

  /**
   * Invoked on a user's request.
   * This method is invoked only if {@link #checkGoal} fails and {@link #willTrySolve} returns true.
   *
   * @param typechecker     a type-checker that can be used to solve the goal
   * @param goalExpression  the original goal expression
   * @param expectedType    the original expected type
   * @param ui              can be used to interact with the user
   * @param callback        a callback for the result;
   *                        if the callback is invoked on {@code null}, an error message will be shown
   */
  default void trySolve(
    @NotNull ExpressionTypechecker typechecker,
    @NotNull ConcreteGoalExpression goalExpression,
    @Nullable CoreExpression expectedType,
    @NotNull ArendUI ui,
    @NotNull Consumer<@Nullable ConcreteExpression> callback) {

    callback.accept(null);
  }
}
