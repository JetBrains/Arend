package org.arend.ext.reference;

import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.error.ErrorReporter;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.function.Function;

public interface ExpressionResolver {
  /**
   * Resolves names in the given expression.
   */
  @NotNull ConcreteExpression resolve(@NotNull ConcreteExpression expression);

  /**
   * Runs {@code action} in a context with {@code refs} removed.
   */
  <T> T hidingRefs(@NotNull Set<? extends ArendRef> refs, @NotNull Function<ExpressionResolver, T> action);

  /**
   * Checks if {@code ref} is unresolved and contains more than 1 name separated by dots.
   */
  boolean isLongUnresolvedReference(@NotNull ArendRef ref);

  @NotNull ErrorReporter getErrorReporter();
}
