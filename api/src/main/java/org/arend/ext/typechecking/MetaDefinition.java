package org.arend.ext.typechecking;

import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.concrete.expr.ConcreteReferenceExpression;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.reference.ExpressionResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A meta definition can execute arbitrary code during the type-checking.
 */
public interface MetaDefinition {
  /**
   * Invoked when the type-checker sees this meta definition.
   *
   * @param typechecker   an instance of the type-checker
   * @param contextData   the context information of the current position of the meta definition
   * @return              the typed expression that will be used as the result of this meta definition
   */
  default @Nullable TypedExpression invokeMeta(@NotNull ExpressionTypechecker typechecker, @NotNull ContextData contextData) {
    return null;
  }

  /**
   * Returns the concrete representation of the result of the meta definition.
   * This method is optional and only used for various features in the IDE.
   *
   * @param arguments   the arguments passed to the meta definition
   * @return            the concrete representation of the result
   */
  default @Nullable ConcreteExpression getConcreteRepresentation(@NotNull List<? extends ConcreteArgument> arguments) {
    return null;
  }

  /**
   * Checks if this meta is applicable can be invoked on the given arguments.
   */
  default boolean checkArguments(@NotNull List<? extends ConcreteArgument> arguments) {
    return true;
  }

  /**
   * Checks if this meta is applicable in the given context.
   */
  default boolean checkContextData(@NotNull ContextData contextData, @NotNull ErrorReporter errorReporter) {
    return true;
  }

  /**
   * Runs additional checks before invoking the definition.
   * This method can be implemented in a base class to simplify the definition of {@code invokeMeta} in subclasses.
   *
   * @param typechecker   an instance of the type-checker
   * @param contextData   the context information of the current position of the meta definition
   * @return              the typed expression that will be used as the result of this meta definition
   */
  default @Nullable TypedExpression checkAndInvokeMeta(@NotNull ExpressionTypechecker typechecker, @NotNull ContextData contextData) {
    return checkContextData(contextData, typechecker.getErrorReporter()) ? invokeMeta(typechecker, contextData) : null;
  }

  /**
   * Runs additional checks before returning the concrete representation of the meta definition.
   * This method can be implemented in a base class to simplify the definition of {@code getConcreteRepresentation} in subclasses.
   *
   * @param arguments   the arguments passed to the meta definition
   * @return            the concrete representation of the result
   */
  default @Nullable ConcreteExpression checkAndGetConcreteRepresentation(@NotNull List<? extends ConcreteArgument> arguments) {
    return checkArguments(arguments) ? getConcreteRepresentation(arguments) : null;
  }

  /**
   * @return true if {@link #resolvePrefix}, {@link #resolveInfix}, and {@link #resolvePostfix} should be invoked.
   */
  default boolean isResolver() {
    return false;
  }

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
