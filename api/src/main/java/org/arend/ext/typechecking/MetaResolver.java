package org.arend.ext.typechecking;

import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.concrete.expr.ConcreteReferenceExpression;
import org.arend.ext.reference.ExpressionResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface MetaResolver {
  /**
   * Resolves names in arguments of the meta.
   * {@code resolver} should be invoked on (an expression containing) every argument that has references.
   *
   * @return a resolved expression.
   */
  default @Nullable ConcreteExpression resolvePrefix(@NotNull ExpressionResolver resolver, @NotNull ConcreteReferenceExpression refExpr, @NotNull List<? extends ConcreteArgument> arguments) {
    return null;
  }

  /**
   * Resolves names in arguments of the meta invoked in the infix form.
   * {@code resolver} should be invoked on (an expression containing) every argument that has references.
   *
   * @return a resolved expression.
   */
  default @Nullable ConcreteExpression resolveInfix(@NotNull ExpressionResolver resolver, @NotNull ConcreteReferenceExpression refExpr, @Nullable ConcreteExpression leftArg, @Nullable ConcreteExpression rightArg) {
    return null;
  }

  /**
   * Resolves names in arguments of the meta invoked in the postfix form.
   * {@code resolver} should be invoked on (an expression containing) every argument that has references.
   *
   * @return a resolved expression.
   */
  default @Nullable ConcreteExpression resolvePostfix(@NotNull ExpressionResolver resolver, @NotNull ConcreteReferenceExpression refExpr, @Nullable ConcreteExpression leftArg, @NotNull List<? extends ConcreteArgument> rightArgs) {
    return null;
  }
}
