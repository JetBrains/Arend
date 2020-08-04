package org.arend.ext.typechecking;

import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.reference.ExpressionResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface MetaResolver {
  /**
   * Resolves names in arguments of the meta.
   * {@code resolver} should be invoked on (an expression containing) every argument that has references.
   *
   * @return a resolved expression.
   */
  default @Nullable ConcreteExpression resolvePrefix(@NotNull ExpressionResolver resolver, @NotNull ContextData contextData) {
    return null;
  }

  /**
   * Resolves names in arguments of the meta invoked in the infix form.
   * {@code resolver} should be invoked on (an expression containing) every argument that has references.
   *
   * @return a resolved expression.
   */
  default @Nullable ConcreteExpression resolveInfix(@NotNull ExpressionResolver resolver, @NotNull ContextData contextData, @Nullable ConcreteExpression leftArg, @Nullable ConcreteExpression rightArg) {
    return null;
  }

  /**
   * Resolves names in arguments of the meta invoked in the postfix form.
   * {@code resolver} should be invoked on (an expression containing) every argument that has references.
   *
   * @return a resolved expression.
   */
  default @Nullable ConcreteExpression resolvePostfix(@NotNull ExpressionResolver resolver, @NotNull ContextData contextData, @Nullable ConcreteExpression leftArg) {
    return null;
  }
}
