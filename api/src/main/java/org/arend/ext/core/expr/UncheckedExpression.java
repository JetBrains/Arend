package org.arend.ext.core.expr;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.ExpressionMapper;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.prettyprinting.PrettyPrintable;
import org.arend.ext.variable.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * An unchecked core expression.
 * It can be checked by {@link org.arend.ext.typechecking.ExpressionTypechecker} to get a checked {@link CoreExpression}.
 */
public interface UncheckedExpression extends PrettyPrintable {
  /**
   * Checks if this expression represents an error expression.
   */
  boolean isError();

  /**
   * Reports the error if this is an error expression.
   *
   * @return {@code true} if this is an error expression.
   */
  boolean reportIfError(@NotNull ErrorReporter errorReporter, @Nullable ConcreteSourceNode marker);

  /**
   * Expressions produces during type-checking may not implement the correct interface.
   * This method returns the underlying expression which always implements it.
   * Note that expressions stored in type-checked definitions are always correct, so this method just returns the expression itself for them.
   */
  @NotNull UncheckedExpression getUnderlyingExpression();

  /**
   * Normalizes expression.
   */
  @NotNull UncheckedExpression normalize(@NotNull NormalizationMode mode);

  /**
   * Unfolds all occurrences of given functions, fields, and variables in this expression.
   *
   * @param variables     a set of functions, fields, and variables to unfold.
   * @param unfolded      variables that are actually unfolded will be added to this set.
   * @param unfoldLet     unfolds \\let expressions if {@code true}.
   * @param unfoldFields  unfolds all fields if {@code true}.
   */
  @NotNull UncheckedExpression unfold(@NotNull Set<? extends Variable> variables, @Nullable Set<Variable> unfolded, boolean unfoldLet, boolean unfoldFields);

  /**
   * Constructs a new expression replacing some subexpressions according to the mapper.
   * The mapper is invoked on every subexpression.
   * If it returns {@code null}, the subexpression won't be changed.
   * If it returns some expression, the subexpression will be replaced with it.
   */
  @Nullable UncheckedExpression replaceSubexpressions(@NotNull ExpressionMapper mapper);

  /**
   * Performs a substitution.
   */
  @NotNull UncheckedExpression substitute(@NotNull Map<? extends CoreBinding, ? extends UncheckedExpression> map);

  /**
   * Compares this expression with another one.
   *
   * @param expr2   another expression
   * @param cmp     indicates whether expressions should be compared on equality or inequality
   * @return        true if expressions are equal; false otherwise
   */
  boolean compare(@NotNull UncheckedExpression expr2, @NotNull CMP cmp);

  /**
   * Returns an expression equivalent to this one in which the given binding does not occur.
   * Returns {@code null} if the binding cannot be eliminated from this expression.
   */
  @Nullable UncheckedExpression removeUnusedBinding(@NotNull CoreBinding binding);

  /**
   * Checks that this expression is equivalent to a lambda expression such that its parameters does not occur in its body.
   * Returns the body of the lambda or {@code null} if this expression is not a lambda or if its parameter occurs in the body.
   */
  @Nullable UncheckedExpression removeConstLam();

  /**
   * Checks if this expression contains the given free binding.
   *
   * @return  true if the expression contains the given binding; false otherwise
   */
  boolean findFreeBinding(@NotNull CoreBinding binding);

  /**
   * Finds a free binding from the given set.
   *
   * @return  a free binding from the given set or {@code null} if there is no such a binding
   */
  @Nullable CoreBinding findFreeBindings(@NotNull Set<? extends CoreBinding> bindings);

  /**
   * @return the set of free variables of this expression.
   */
  @NotNull Set<? extends CoreBinding> findFreeBindings();

  /**
   * Checks if this expression and {@param expression} are disjoint constructors of the same data type.
   */
  boolean areDisjointConstructors(@NotNull UncheckedExpression expression);
}
