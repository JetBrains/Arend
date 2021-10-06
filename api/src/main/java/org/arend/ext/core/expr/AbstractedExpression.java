package org.arend.ext.core.expr;

import org.arend.ext.core.context.CoreBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Represents an expression in a context.
 * The main operation that can be performed on such expressions is {@link org.arend.ext.typechecking.ExpressionTypechecker#substituteAbstractedExpression}.
 */
public interface AbstractedExpression {
  int getNumberOfAbstractedBindings();
  @Nullable CoreBinding findFreeBinding(@NotNull Set<? extends CoreBinding> bindings);
}
