package org.arend.term.abs;

import org.arend.naming.reference.Referable;
import org.arend.term.Fixity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.Collection;

public class BaseAbstractExpressionVisitor<P, R> implements AbstractExpressionVisitor<P, R> {
  private R defaultValue;

  public BaseAbstractExpressionVisitor(R defaultValue) {
    this.defaultValue = defaultValue;
  }

  @Override
  public R visitReference(@Nullable Object data, @NotNull Referable referent, @Nullable Fixity fixity, @Nullable Abstract.LevelExpression level1, @Nullable Abstract.LevelExpression level2, P params) {
    return defaultValue;
  }

  @Override
  public R visitReference(@Nullable Object data, @NotNull Referable referent, int lp, int lh, P params) {
    return defaultValue;
  }

  @Override
  public R visitThis(@Nullable Object data, P params) {
    return defaultValue;
  }

  @Override
  public R visitLam(@Nullable Object data, @NotNull Collection<? extends Abstract.Parameter> parameters, @Nullable Abstract.Expression body, P params) {
    return defaultValue;
  }

  @Override
  public R visitPi(@Nullable Object data, @NotNull Collection<? extends Abstract.Parameter> parameters, @Nullable Abstract.Expression codomain, P params) {
    return defaultValue;
  }

  @Override
  public R visitUniverse(@Nullable Object data, @Nullable Integer pLevelNum, @Nullable Integer hLevelNum, @Nullable Abstract.LevelExpression pLevel, @Nullable Abstract.LevelExpression hLevel, P params) {
    return defaultValue;
  }

  @Override
  public R visitInferHole(@Nullable Object data, P params) {
    return defaultValue;
  }

  @Override
  public R visitGoal(@Nullable Object data, @Nullable String name, @Nullable Abstract.Expression expression, P params) {
    return defaultValue;
  }

  @Override
  public R visitApplyHole(@Nullable Object data, P params) {
    return defaultValue;
  }

  @Override
  public R visitTuple(@Nullable Object data, @NotNull Collection<? extends Abstract.Expression> fields, @Nullable Object trailingComma, P params) {
    return defaultValue;
  }

  @Override
  public R visitSigma(@Nullable Object data, @NotNull Collection<? extends Abstract.Parameter> parameters, P params) {
    return defaultValue;
  }

  @Override
  public R visitBinOpSequence(@Nullable Object data, @NotNull Abstract.Expression left, @NotNull Collection<? extends Abstract.BinOpSequenceElem> sequence, P params) {
    return defaultValue;
  }

  @Override
  public R visitCase(@Nullable Object data, boolean isSFunc, @Nullable Abstract.EvalKind evalKind, @NotNull Collection<? extends Abstract.CaseArgument> caseArgs, @Nullable Abstract.Expression resultType, @Nullable Abstract.Expression resultTypeLevel, @NotNull Collection<? extends Abstract.FunctionClause> clauses, P params) {
    return defaultValue;
  }

  @Override
  public R visitFieldAccs(@Nullable Object data, @NotNull Abstract.Expression expression, @NotNull Collection<Integer> fieldAccs, P params) {
    return defaultValue;
  }

  @Override
  public R visitClassExt(@Nullable Object data, boolean isNew, @Nullable Abstract.EvalKind evalKind, @Nullable Abstract.Expression baseClass, @Nullable Collection<? extends Abstract.ClassFieldImpl> implementations, @NotNull Collection<? extends Abstract.BinOpSequenceElem> sequence, P params) {
    return defaultValue;
  }

  @Override
  public R visitLet(@Nullable Object data, boolean isStrict, @NotNull Collection<? extends Abstract.LetClause> clauses, @Nullable Abstract.Expression expression, P params) {
    return defaultValue;
  }

  @Override
  public R visitNumericLiteral(@Nullable Object data, @NotNull BigInteger number, P params) {
    return defaultValue;
  }

  @Override
  public R visitStringLiteral(@Nullable Object data, @NotNull String unescapedString, P params) {
    return defaultValue;
  }

  @Override
  public R visitTyped(@Nullable Object data, @NotNull Abstract.Expression expr, @NotNull Abstract.Expression type, P params) {
    return defaultValue;
  }
}
