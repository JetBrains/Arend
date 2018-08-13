package com.jetbrains.jetpad.vclang.term.abs;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Collection;

public class BaseAbstractExpressionVisitor<P, R> implements AbstractExpressionVisitor<P, R> {
  private R defaultValue;

  public BaseAbstractExpressionVisitor(R defaultValue) {
    this.defaultValue = defaultValue;
  }

  @Override
  public boolean visitErrors() {
    return false;
  }

  @Override
  public R visitReference(@Nullable Object data, @Nonnull Referable referent, @Nullable Abstract.LevelExpression level1, @Nullable Abstract.LevelExpression level2, @Nullable Abstract.ErrorData errorData, P params) {
    return defaultValue;
  }

  @Override
  public R visitReference(@Nullable Object data, @Nonnull Referable referent, int lp, int lh, @Nullable Abstract.ErrorData errorData, P params) {
    return defaultValue;
  }

  @Override
  public R visitLam(@Nullable Object data, @Nonnull Collection<? extends Abstract.Parameter> parameters, @Nullable Abstract.Expression body, @Nullable Abstract.ErrorData errorData, P params) {
    return defaultValue;
  }

  @Override
  public R visitPi(@Nullable Object data, @Nonnull Collection<? extends Abstract.Parameter> parameters, @Nullable Abstract.Expression codomain, @Nullable Abstract.ErrorData errorData, P params) {
    return defaultValue;
  }

  @Override
  public R visitUniverse(@Nullable Object data, @Nullable Integer pLevelNum, @Nullable Integer hLevelNum, @Nullable Abstract.LevelExpression pLevel, @Nullable Abstract.LevelExpression hLevel, @Nullable Abstract.ErrorData errorData, P params) {
    return defaultValue;
  }

  @Override
  public R visitInferHole(@Nullable Object data, @Nullable Abstract.ErrorData errorData, P params) {
    return defaultValue;
  }

  @Override
  public R visitGoal(@Nullable Object data, @Nullable String name, @Nullable Abstract.Expression expression, @Nullable Abstract.ErrorData errorData, P params) {
    return defaultValue;
  }

  @Override
  public R visitTuple(@Nullable Object data, @Nonnull Collection<? extends Abstract.Expression> fields, @Nullable Abstract.ErrorData errorData, P params) {
    return defaultValue;
  }

  @Override
  public R visitSigma(@Nullable Object data, @Nonnull Collection<? extends Abstract.Parameter> parameters, @Nullable Abstract.ErrorData errorData, P params) {
    return defaultValue;
  }

  @Override
  public R visitBinOpSequence(@Nullable Object data, @Nonnull Abstract.Expression left, @Nonnull Collection<? extends Abstract.BinOpSequenceElem> sequence, @Nullable Abstract.ErrorData errorData, P params) {
    return defaultValue;
  }

  @Override
  public R visitCase(@Nullable Object data, @Nonnull Collection<? extends Abstract.Expression> expressions, @Nonnull Collection<? extends Abstract.FunctionClause> clauses, @Nullable Abstract.ErrorData errorData, P params) {
    return defaultValue;
  }

  @Override
  public R visitFieldAccs(@Nullable Object data, @Nonnull Abstract.Expression expression, @Nonnull Collection<Integer> fieldAccs, @Nullable Abstract.ErrorData errorData, P params) {
    return defaultValue;
  }

  @Override
  public R visitClassExt(@Nullable Object data, boolean isNew, @Nullable Abstract.Expression baseClass, @Nullable Collection<? extends Abstract.ClassFieldImpl> implementations, @Nonnull Collection<? extends Abstract.BinOpSequenceElem> sequence, @Nullable Abstract.ErrorData errorData, P params) {
    return defaultValue;
  }

  @Override
  public R visitLet(@Nullable Object data, @Nonnull Collection<? extends Abstract.LetClause> clauses, @Nullable Abstract.Expression expression, @Nullable Abstract.ErrorData errorData, P params) {
    return defaultValue;
  }

  @Override
  public R visitNumericLiteral(@Nullable Object data, @Nonnull BigInteger number, @Nullable Abstract.ErrorData errorData, P params) {
    return defaultValue;
  }
}
