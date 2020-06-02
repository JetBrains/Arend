package org.arend.ext.typechecking;

import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.concrete.expr.ConcreteReferenceExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents the context information of the current position of a meta definition.
 */
public interface ContextData {
  /**
   * If the definition was explicitly invoked from code,
   * returns the reference expression corresponding to this invocation.
   */
  @NotNull ConcreteReferenceExpression getReferenceExpression();

  /**
   * Returns the arguments passed to the meta definition.
   */
  @NotNull List<? extends ConcreteArgument> getArguments();

  void setArguments(@NotNull List<? extends ConcreteArgument> arguments);

  /**
   * Returns the expected type in the current context.
   * The meta definition should return an expression of this type.
   * It can be {@code null} if the expected type is unknown.
   */
  CoreExpression getExpectedType();

  void setExpectedType(@Nullable CoreExpression expectedType);

  default Object getUserData() {
    return null;
  }

  default void setUserData(Object userData) {}
}
