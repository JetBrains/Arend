package org.arend.ext.concrete.expr;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

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

  /**
   * Performs a substitution.
   */
  @NotNull ConcreteExpression substitute(@NotNull Map<ArendRef, ConcreteExpression> substitution);

  <P, R> R accept(@NotNull ConcreteVisitor<? super P, ? extends R> visitor, P params);
}
