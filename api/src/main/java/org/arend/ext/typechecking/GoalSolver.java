package org.arend.ext.typechecking;

import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.concrete.expr.ConcreteGoalExpression;
import org.arend.ext.core.expr.CoreExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * A goal solver provides method {@link #checkGoal} which can be used to typecheck the expression in the goal and apply some simple transformations to it.
 * It also provides a list of additional solvers which can be used to do more complicated computations, proof search, and interaction with the user.
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

  default @NotNull Collection<? extends InteractiveGoalSolver> getAdditionalSolvers() {
    return Collections.emptyList();
  }
}
