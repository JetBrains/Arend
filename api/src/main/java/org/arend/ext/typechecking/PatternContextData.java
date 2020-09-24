package org.arend.ext.typechecking;

import org.arend.ext.concrete.ConcretePattern;
import org.arend.ext.core.expr.CoreExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the context information of the current position of a pattern.
 *
 * @see ContextData
 */
public interface PatternContextData {
  /**
   * @see ContextData#getMarker()
   */
  @NotNull ConcretePattern getMarker();

  /**
   * @see ContextData#getExpectedType()
   */
  CoreExpression getExpectedType();

  /**
   * @see ContextData#setExpectedType(CoreExpression)
   */
  void setExpectedType(@Nullable CoreExpression expectedType);
}
