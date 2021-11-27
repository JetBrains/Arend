package org.arend.extImpl;

import org.arend.core.expr.Expression;
import org.arend.core.subst.UnfoldVisitor;
import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.expr.UncheckedExpression;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.ExpressionMapper;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.variable.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public class UncheckedExpressionImpl implements UncheckedExpression {
  private final Expression myExpression;

  public UncheckedExpressionImpl(Expression expression) {
    myExpression = expression;
  }

  public static Expression extract(UncheckedExpression expr) {
    if (expr == null || expr instanceof Expression) {
      return (Expression) expr;
    }
    if (expr instanceof UncheckedExpressionImpl) {
      return ((UncheckedExpressionImpl) expr).myExpression;
    }
    throw new IllegalArgumentException();
  }

  private static UncheckedExpression wrap(UncheckedExpression expr) {
    return expr instanceof Expression ? new UncheckedExpressionImpl((Expression) expr) : expr;
  }

  @Override
  public boolean isError() {
    return myExpression.isError();
  }

  @Override
  public boolean reportIfError(@NotNull ErrorReporter errorReporter, @Nullable ConcreteSourceNode marker) {
    return myExpression.reportIfError(errorReporter, marker);
  }

  @Override
  public @NotNull UncheckedExpression getUnderlyingExpression() {
    return wrap(myExpression.getUnderlyingExpression());
  }

  @Override
  public @NotNull UncheckedExpression normalize(@NotNull NormalizationMode mode) {
    return wrap(myExpression.normalize(mode));
  }

  @Override
  public @NotNull UncheckedExpression unfold(@NotNull Set<? extends Variable> variables, @Nullable Set<Variable> unfolded, boolean unfoldLet, boolean unfoldFields) {
    return variables.isEmpty() && !unfoldLet && !unfoldFields ? this : wrap(myExpression.accept(new UnfoldVisitor(variables, unfolded, unfoldLet, unfoldFields), null));
  }

  @Override
  public @Nullable UncheckedExpression replaceSubexpressions(@NotNull ExpressionMapper mapper) {
    return wrap(myExpression.replaceSubexpressions(mapper));
  }

  @Override
  public boolean compare(@NotNull UncheckedExpression expr2, @NotNull CMP cmp) {
    return myExpression.compare(expr2, cmp);
  }

  @Override
  public @NotNull UncheckedExpression substitute(@NotNull Map<? extends CoreBinding, ? extends UncheckedExpression> map) {
    return wrap(myExpression.substitute(map));
  }

  @Override
  public @Nullable UncheckedExpression removeUnusedBinding(@NotNull CoreBinding binding) {
    return wrap(myExpression.removeUnusedBinding(binding));
  }

  @Override
  public @Nullable UncheckedExpression removeConstLam() {
    return wrap(myExpression.removeConstLam());
  }

  @Override
  public boolean findFreeBinding(@NotNull CoreBinding binding) {
    return myExpression.findFreeBinding(binding);
  }

  @Override
  public @Nullable CoreBinding findFreeBindings(@NotNull Set<? extends CoreBinding> bindings) {
    return myExpression.findFreeBindings(bindings);
  }

  @Override
  public @NotNull Set<? extends CoreBinding> findFreeBindings() {
    return myExpression.findFreeBindings();
  }

  @Override
  public boolean areDisjointConstructors(@NotNull UncheckedExpression expression) {
    return myExpression.areDisjointConstructors(expression);
  }

  @Override
  public String toString() {
    return myExpression.toString();
  }
}
