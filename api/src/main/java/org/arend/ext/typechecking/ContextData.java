package org.arend.ext.typechecking;

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
   * Returns the reference expression corresponding to the meta definition itself.
   * Can be used as a marked for errors when a more specific position of the error is not available.
   */
  @NotNull ConcreteReferenceExpression getReferenceExpression();

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
