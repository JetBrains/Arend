package org.arend.ext.concrete.expr;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Represents a concrete expression.
 * Concrete expressions represent Arend code as it looks from the user perspective.
 * Can be checked and transformed into a {@link org.arend.ext.core.expr.CoreExpression} by {@link org.arend.ext.typechecking.ExpressionTypechecker}.
 */
public interface ConcreteExpression extends ConcreteSourceNode {
  /**
   * Performs a substitution.
   */
  @NotNull ConcreteExpression substitute(@NotNull Map<ArendRef, ConcreteExpression> substitution);

  <P, R> R accept(@NotNull ConcreteVisitor<? super P, ? extends R> visitor, P params);
}
