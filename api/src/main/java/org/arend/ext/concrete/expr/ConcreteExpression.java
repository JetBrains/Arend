package org.arend.ext.concrete.expr;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents a concrete expression.
 * Concrete expressions represent Arend code as it looks from the user perspective.
 * Can be checked and transformed into a {@link org.arend.ext.core.expr.CoreExpression} by {@link org.arend.ext.typechecking.ExpressionTypechecker}.
 */
public interface ConcreteExpression extends ConcreteSourceNode {
  /**
   * If this expression is of the form `f a_1 ... a_n`, returns the list `(f, a_1, ... a_n)`.
   * The result list is always non-empty and the first element is always an explicit argument.
   */
  @NotNull List<ConcreteArgument> getArgumentsSequence();
}
