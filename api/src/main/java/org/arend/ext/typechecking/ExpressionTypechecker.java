package org.arend.ext.typechecking;

import org.arend.ext.FreeBindingsModifier;
import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.definition.CoreClassDefinition;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.core.expr.UncheckedExpression;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * A type-checker is used to check and transform {@link ConcreteExpression} and {@link UncheckedExpression} to {@link CoreExpression}.
 */
public interface ExpressionTypechecker {
  /**
   * Returns the list of bindings available in the given context.
   */
  @NotNull List<CoreBinding> getFreeBindingsList();

  /**
   * Returns the free binding corresponding to the given reference.
   */
  @Nullable CoreBinding getFreeBinding(@NotNull ArendRef ref);

  /**
   * All errors that occur during invocation of a meta definition should be reported through the reporter returned by this method.
   */
  @NotNull ErrorReporter getErrorReporter();

  /**
   * Checks the specified concrete expression.
   *
   * @param expression    a concrete expression that should be checked
   * @param expectedType  the type of the concrete expression or {@code null} if the expected type is unknown
   * @return              the typed core expression corresponding to the given concrete expression
   */
  @Nullable TypedExpression typecheck(@NotNull ConcreteExpression expression, @Nullable CoreExpression expectedType);

  /**
   * Checks the specified unchecked core expression.
   *
   * @param expression    a core expression that should be checked
   * @param sourceNode    a marker that will be used for error reporting
   * @return              the typed core expression corresponding to the given unchecked expression
   */
  @Nullable TypedExpression check(@NotNull UncheckedExpression expression, @NotNull ConcreteSourceNode sourceNode);

  /**
   * Defers the invocation of the given meta.
   * This might be useful if the meta definition fails because some inference variable are not inferred at the given time.
   *
   * @param meta          a meta definition that should be deferred
   * @param contextData   a context that will be passed to deferred meta when it will be invoked
   * @param type          the type of the returned expression
   * @return              a typed expression which represents the deferred meta with {@code type} as its type
   */
  @Nullable TypedExpression defer(@NotNull MetaDefinition meta, @NotNull ContextData contextData, @NotNull CoreExpression type);

  /**
   * Compares two expressions.
   * It might produce some equations on inference variables that stored in the type-checker.
   *
   * @param expr1           the first expression to compare
   * @param expr2           the second expression to compare
   * @param cmp             indicates whether expressions should be compared on equality or inequality
   * @param marker          a marker that will be used in errors if the equations produced by this comparison won't be satisfied
   * @param allowEquations  true if this method should produce equations.
   *                        If this parameter is false and there are some equations that should be generated, the method will return false immediately
   * @param normalize       true if expressions and their subexpressions should be normalized during comparison
   * @return                true if expressions are equal modulo equations on inference variables; false otherwise
   */
  boolean compare(@NotNull UncheckedExpression expr1, @NotNull UncheckedExpression expr2, @NotNull CMP cmp, @Nullable ConcreteSourceNode marker, boolean allowEquations, boolean normalize);

  /**
   * Invokes the specified action with the specified error reporter.
   * Can be used to suppress errors or handle them in some specific way.
   */
  <T> T withErrorReporter(@NotNull ErrorReporter errorReporter, @NotNull Function<ExpressionTypechecker, T> action);

  /**
   * Invokes the specified action with modified set of free bindings
   */
  <T> T withFreeBindings(@NotNull FreeBindingsModifier modifier, @NotNull Function<ExpressionTypechecker, T> action);

  /**
   * Searches for an instance of the specified class.
   *
   * @param classDefinition         the method will search for an instance of this class
   * @param classifyingExpression   the classifying expression of the instance
   * @param sourceNode              a marker for errors that might occur during the search
   * @return                        an instance of the given class with the specified classifying expression or {@code null} if there is no such instance
   */
  @Nullable ConcreteExpression findInstance(@NotNull CoreClassDefinition classDefinition, @Nullable UncheckedExpression classifyingExpression, @NotNull ConcreteSourceNode sourceNode);

  /**
   * Searches for an instance of the specified class.
   *
   * @param classDefinition         the method will search for an instance of this class
   * @param classifyingExpression   the classifying expression of the instance
   * @param expectedType            the expected type of the instance
   * @param sourceNode              a marker for errors that might occur during the search
   * @return                        an instance of the given class with the specified classifying expression or {@code null} if there is no such instance
   */
  @Nullable TypedExpression findInstance(@NotNull CoreClassDefinition classDefinition, @Nullable UncheckedExpression classifyingExpression, @Nullable CoreExpression expectedType, @NotNull ConcreteSourceNode sourceNode);

  /**
   * Checks if the type-checking was cancelled.
   * Should be invoked often during heavy computations.
   */
  void checkCancelled();
}
