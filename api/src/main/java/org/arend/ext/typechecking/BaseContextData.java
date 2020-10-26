package org.arend.ext.typechecking;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.core.expr.CoreExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface BaseContextData {
  /**
   * Returns the expected type in the current context.
   * It can be {@code null} if the expected type is unknown.
   */
  CoreExpression getExpectedType();

  void setExpectedType(@Nullable CoreExpression expectedType);

  /**
   * A marker that can be used for error reporting.
   */
  @NotNull ConcreteSourceNode getMarker();
}
