package org.arend.ext.typechecking;

import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.concrete.expr.ConcreteGoalExpression;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.concrete.expr.ConcreteReferenceExpression;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents the context information of the current position of a meta definition.
 */
public interface ContextData {
  /**
   * This expression be used as a marker for errors when a more specific position of the error is not available.
   */
  @NotNull ConcreteExpression getMarker();

  /**
   * If the definition was explicitly invoked from code,
   * returns the reference expression corresponding to this invocation.
   */
  ConcreteReferenceExpression getReferenceExpression();

  /**
   * If the definition is invoked as a goal solver,
   * returns the goal expression corresponding to this invocation.
   */
  ConcreteGoalExpression getGoalExpression();

  /**
   * Returns the arguments passed to the meta definition.
   */
  @NotNull List<? extends ConcreteArgument> getArguments();

  /**
   * Sets the argument to the meta definition.
   */
  void setArguments(@NotNull List<? extends ConcreteArgument> arguments);

  /**
   * Returns the expected type in the current context.
   * The meta definition should return an expression of this type.
   * It can be {@code null} if the expected type is unknown.
   */
  CoreExpression getExpectedType();
}
