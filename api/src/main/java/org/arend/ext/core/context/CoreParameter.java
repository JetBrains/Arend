package org.arend.ext.core.context;

import org.arend.ext.core.expr.AbstractedExpression;
import org.arend.ext.core.expr.CoreExpression;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an element of a linked list of parameters of a definition, lambda, pi, etc.
 */
public interface CoreParameter {
  boolean isExplicit();

  /**
   * Returns the underlying binding of the parameter.
   */
  @NotNull CoreBinding getBinding();

  /**
   * Returns the type of the parameter.
   * Equivalent to {@code getBinding().getTypeExpr()}
   */
  @NotNull CoreExpression getTypeExpr();

  /**
   * Checks if this parameter is the last one.
   *
   * @return  true if the parameter is not the last one; false otherwise
   */
  boolean hasNext();

  /**
   * Returns the next parameter; throws an exception if this parameter is the last one.
   */
  @NotNull CoreParameter getNext();

  /**
   * Abstracts the type of {@code size}-th parameter.
   *
   * @return the abstracted type in a context of length {@code size}.
   */
  @NotNull AbstractedExpression abstractType(int size);
}
