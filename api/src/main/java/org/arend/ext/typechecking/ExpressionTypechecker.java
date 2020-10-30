package org.arend.ext.typechecking;

import org.arend.ext.FreeBindingsModifier;
import org.arend.ext.concrete.ConcreteNumberPattern;
import org.arend.ext.concrete.ConcreteParameter;
import org.arend.ext.concrete.ConcretePattern;
import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.concrete.expr.ConcreteLamExpression;
import org.arend.ext.core.body.CorePattern;
import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.context.CoreInferenceVariable;
import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.core.definition.CoreClassDefinition;
import org.arend.ext.core.expr.AbstractedExpression;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.core.expr.CoreInferenceReferenceExpression;
import org.arend.ext.core.expr.UncheckedExpression;
import org.arend.ext.core.level.CoreSort;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.SubstitutionPair;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.instance.InstanceSearchParameters;
import org.arend.ext.instance.SubclassSearchParameters;
import org.arend.ext.reference.ArendRef;
import org.arend.ext.userData.UserDataHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A type-checker is used to check and transform {@link ConcreteExpression} and {@link UncheckedExpression} to {@link CoreExpression}.
 */
public interface ExpressionTypechecker extends UserDataHolder {
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
   * @param sourceNode    a marker for error reporting
   * @return              the typed core expression corresponding to the given unchecked expression
   */
  @Nullable TypedExpression check(@NotNull UncheckedExpression expression, @NotNull ConcreteSourceNode sourceNode);

  /**
   * Replaces the type in the given typed expression.
   * The original type should be less than or equal to the new type.
   * Otherwise, {@code null} is returned.
   */
  @Nullable TypedExpression replaceType(@NotNull TypedExpression typedExpression, @NotNull CoreExpression type, @Nullable ConcreteSourceNode marker);

  /**
   * Checks the specified concrete typed parameters.
   *
   * @param parameters  a list of parameters.
   *                    All parameters in the list must by typed.
   */
  @Nullable CoreParameter typecheckParameters(@NotNull Collection<? extends ConcreteParameter> parameters);

  /**
   * Checks the specified concrete patterns.
   *
   * @param patterns    a list of concrete patterns that should be checked.
   * @param parameters  parameters that specify the type of patterns.
   *                    The number of parameters must be the same as the number of patterns.
   * @param marker      a marker for error reporting.
   * @return a list of core patterns or {@code null} if typechecking fails.
   */
  @Nullable List<CorePattern> typecheckPatterns(@NotNull Collection<? extends ConcretePattern> patterns, @NotNull CoreParameter parameters, @NotNull ConcreteSourceNode marker);

  /**
   * Translate a concrete number pattern into combination of <code>zero</code> and <code>suc</code> patterns.
   *
   * @param pattern a concrete number pattern.
   * @return a translated pattern.
   */
  @NotNull ConcretePattern desugarNumberPattern(@NotNull ConcreteNumberPattern pattern);

  /**
   * Typechecks a lambda expression using {code parameters} as types of its parameters.
   */
  @Nullable TypedExpression typecheckLambda(@NotNull ConcreteLamExpression expr, @NotNull CoreParameter parameters);

  /**
   * @return a list of explicit parameters with specified types, or {@code null} if one of the expressions in {@code types} is not a type.
   */
  @NotNull CoreParameter makeParameters(@NotNull List<? extends CoreExpression> types, @NotNull ConcreteSourceNode marker);

  /**
   * Merges a list of parameters.
   */
  @NotNull CoreParameter mergeParameters(@NotNull List<? extends CoreParameter> parameters);

  /**
   * Typechecks expressions in {@code substitution} and substitutes them into {@code expression}.
   * Also, substitutes {@code sort} if it is not null.
   * Bindings in {@code substitution} must be listed in the same order they are defined.
   */
  @Nullable CoreExpression substitute(@NotNull CoreExpression expression, @Nullable CoreSort sort, @NotNull List<SubstitutionPair> substitution);

  /**
   * Typechecks {@code arguments} and substitutes them into {@code expression}.
   * Also, substitutes {@code sort} if it is not null.
   * The number of {@code arguments} should be less than or equal to the length of the context of {@code expression}.
   */
  @Nullable AbstractedExpression substituteAbstractedExpression(@NotNull AbstractedExpression expression, @Nullable CoreSort sort, @NotNull List<? extends ConcreteExpression> arguments);

  /**
   * Typechecks {@code arguments} and substitutes them into {@code parameters}.
   * Also, substitutes {@code sort} if it is not null.
   * Some elements of {@code arguments} may be {@code null}; the corresponding parameters will be added to the result.
   * The size of {@code arguments} should be less than or equal to the size of {@code parameters}.
   * The size of the result is the size of {@code parameters} minus the number of non-null elements of {@code arguments}.
   */
  @Nullable CoreParameter substituteParameters(@NotNull CoreParameter parameters, @Nullable CoreSort sort, @NotNull List<? extends ConcreteExpression> arguments);

  enum Stage { BEFORE_SOLVER, BEFORE_LEVELS, AFTER_LEVELS }

  /**
   * Defers the invocation of the given meta.
   * This might be useful if the meta definition fails because some inference variable are not inferred at the given time.
   *
   * @param meta          a meta definition that should be deferred
   * @param contextData   a context that will be passed to deferred meta when it will be invoked
   * @param type          the type of the returned expression
   * @return              a typed expression which represents the deferred meta with {@code type} as its type
   */
  @Nullable TypedExpression defer(@NotNull MetaDefinition meta, @NotNull ContextData contextData, @NotNull CoreExpression type, @NotNull Stage stage);

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
   * Saves the state of the typechecker and executes {@code action}.
   */
  <T> T withCurrentState(@NotNull Function<ExpressionTypechecker, T> action);

  /**
   * Sets the saved state to the current state.
   * This function can be invoked only from within the action passed to {@link #withCurrentState}.
   * It can be invoked multiple times.
   */
  void updateSavedState();

  /**
   * Loads the saved state.
   * This function can be invoked only from within the action passed to {@link #withCurrentState}.
   * It can be invoked multiple times.
   */
  void loadSavedState();

  /**
   * Solves an inference variable.
   *
   * @return true if the variable was successfully solved;
   *         false is returned if the variable was solved before, if {@code expression} has a wrong type,
   *         or if it has free variables that are not in the context of the variable.
   */
  boolean solveInferenceVariable(@NotNull CoreInferenceVariable variable, @NotNull CoreExpression expression);

  /**
   * Creates a new inference variable.
   *
   * @param name                      a name of the variable; used only for printing.
   * @param type                      a type of the variable; when the variable is solved the type of the expression should match this type.
   * @param marker                    a marker that is used to report errors related to this variable.
   * @param isSolvableFromEquations   if true, then the variable can be solved by the equation solver;
   *                                  otherwise, it must be explicitly solved by invoking {@link #solveInferenceVariable}
   * @return a reference expression with a new inference variable.
   */
  @NotNull CoreInferenceReferenceExpression generateNewInferenceVariable(@NotNull String name, @NotNull CoreExpression type, @NotNull ConcreteSourceNode marker, boolean isSolvableFromEquations);

  /**
   * Creates a sort from a pair of fresh level inference variables.
   *
   * @param marker                    a marker that is used to report errors related to generated variables.
   */
  @NotNull CoreSort generateSort(@NotNull ConcreteSourceNode marker);

  /**
   * Searches for an instance of the specified class.
   *
   * @param classDefinition         the method will search for an instance of this class
   * @param classifyingExpression   the classifying expression of the instance
   * @param sourceNode              a marker for errors that might occur during the search
   * @return                        an instance of the given class with the specified classifying expression or {@code null} if there is no such instance
   */
  default @Nullable ConcreteExpression findInstance(@NotNull CoreClassDefinition classDefinition, @Nullable UncheckedExpression classifyingExpression, @NotNull ConcreteSourceNode sourceNode) {
    return findInstance(new SubclassSearchParameters(classDefinition), classifyingExpression, sourceNode);
  }

  /**
   * Searches for an instance of the specified class.
   *
   * @param classDefinition         the method will search for an instance of this class
   * @param classifyingExpression   the classifying expression of the instance
   * @param expectedType            the expected type of the instance
   * @param sourceNode              a marker for errors that might occur during the search
   * @return                        an instance of the given class with the specified classifying expression or {@code null} if there is no such instance
   */
  default @Nullable TypedExpression findInstance(@NotNull CoreClassDefinition classDefinition, @Nullable UncheckedExpression classifyingExpression, @Nullable CoreExpression expectedType, @NotNull ConcreteSourceNode sourceNode) {
    return findInstance(new SubclassSearchParameters(classDefinition), classifyingExpression, expectedType, sourceNode);
  }

  /**
   * Searches for an instance of a class satisfying the specified predicate.
   *
   * @param parameters              specifies parameters for the search
   * @param classifyingExpression   the classifying expression of the instance
   * @param sourceNode              a marker for errors that might occur during the search
   * @return                        an instance of the given class with the specified classifying expression or {@code null} if there is no such instance
   */
  @Nullable ConcreteExpression findInstance(@NotNull InstanceSearchParameters parameters, @Nullable UncheckedExpression classifyingExpression, @NotNull ConcreteSourceNode sourceNode);

  /**
   * Searches for an instance of a class satisfying the specified predicate.
   *
   * @param parameters              specifies parameters for the search
   * @param classifyingExpression   the classifying expression of the instance
   * @param expectedType            the expected type of the instance
   * @param sourceNode              a marker for errors that might occur during the search
   * @return                        an instance of the given class with the specified classifying expression or {@code null} if there is no such instance
   */
  @Nullable TypedExpression findInstance(@NotNull InstanceSearchParameters parameters, @Nullable UncheckedExpression classifyingExpression, @Nullable CoreExpression expectedType, @NotNull ConcreteSourceNode sourceNode);

  /**
   * @return either natural or integer representation of {@code number}.
   *         The type depends on the sign of {@code number} and on {@code expectedType}.
   */
  @Nullable TypedExpression checkNumber(@NotNull BigInteger number, @Nullable CoreExpression expectedType, @NotNull ConcreteExpression marker);

  /**
   * Checks if the type-checking was cancelled.
   * Should be invoked often during heavy computations.
   */
  void checkCancelled();
}
