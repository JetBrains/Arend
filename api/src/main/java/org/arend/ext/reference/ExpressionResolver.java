package org.arend.ext.reference;

import org.arend.ext.concrete.ResolvedApplication;
import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.error.ErrorReporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public interface ExpressionResolver {
  /**
   * Resolves names in the given expression.
   */
  @NotNull ConcreteExpression resolve(@NotNull ConcreteExpression expression);

  /**
   * Resolves the expression corresponding to a sequence of arguments.
   *
   * @param arguments   a non-empty sequence of arguments
   */
  @NotNull ConcreteExpression resolve(@Nullable Object data, @NotNull List<? extends ConcreteArgument> arguments);

  /**
   * Resolves an application expression. If the given expression is not an unresolved sequence, returns {@code null}.
   */
  @Nullable ResolvedApplication resolveApplication(@NotNull ConcreteExpression expression);

  /**
   * Constructs a new resolver in which {@code refs} are removed from the context.
   */
  @NotNull ExpressionResolver hideRefs(@NotNull Set<? extends ArendRef> refs);

  /**
   * Constructs a new resolver in which {@code refs} are added to the context.
   *
   * @param allowContext  if true, adds {@code refs} to the current context; otherwise, replaces the context.
   */
  @NotNull ExpressionResolver useRefs(@NotNull List<? extends ArendRef> refs, boolean allowContext);

  /**
   * Registers a reference as a declaration.
   * This method should be invoked if the reference is not resolved as a declaration, but should be treated as such.
   */
  void registerDeclaration(@NotNull ArendRef ref);

  /**
   * Checks if {@code ref} is unresolved and contains more than 1 name separated by dots.
   */
  boolean isLongUnresolvedReference(@NotNull ArendRef ref);

  @NotNull ErrorReporter getErrorReporter();
}
