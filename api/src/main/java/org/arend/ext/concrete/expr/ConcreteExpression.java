package org.arend.ext.concrete.expr;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Represents a concrete expression.
 * Concrete expressions represent Arend code as it looks from the user perspective.
 * Can be checked and transformed into a {@link org.arend.ext.core.expr.CoreExpression} by {@link org.arend.ext.typechecking.ExpressionTypechecker}.
 */
public interface ConcreteExpression extends ConcreteSourceNode {
}
