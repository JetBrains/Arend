package org.arend.ext.typechecking;

import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.concrete.expr.ConcreteGoalExpression;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.ui.ArendUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface InteractiveGoalSolver {
  /**
   * @return a short description of the solver which will be shown to the user.
   */
  @NotNull String getShortDescription();

  /**
   * @return true if {@link #solve} should be invoked, false otherwise.
   */
  default boolean isApplicable(@NotNull ConcreteGoalExpression goalExpression, @Nullable CoreExpression expectedType) {
    return false;
  }

  /**
   * Invoked on a user's request.
   * This method is invoked only if {@link #isApplicable} returns true.
   *
   * @param typechecker     a type-checker that can be used to solve the goal
   * @param goalExpression  the original goal expression
   * @param expectedType    the original expected type
   * @param ui              can be used for immediate interaction;
   *                        it shouldn't be used during long computations
   * @param callback        a callback for the result;
   *                        if the callback is invoked on {@code null}, an error message will be shown
   */
  default void solve(
    @NotNull ExpressionTypechecker typechecker,
    @NotNull ConcreteGoalExpression goalExpression,
    @Nullable CoreExpression expectedType,
    @NotNull ArendUI ui,
    @NotNull Consumer<ConcreteExpression> callback) {

    callback.accept(null);
  }
}
