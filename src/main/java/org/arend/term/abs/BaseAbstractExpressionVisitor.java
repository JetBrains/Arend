package org.arend.term.abs;

import org.arend.naming.reference.Referable;
import org.arend.term.Fixity;

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
  public R visitReference(@Nullable Object data, @Nonnull Referable referent, @Nullable Fixity fixity, @Nullable Abstract.LevelExpression level1, @Nullable Abstract.LevelExpression level2, P params) {
    return defaultValue;
  }

  @Override
  public R visitReference(@Nullable Object data, @Nonnull Referable referent, int lp, int lh, P params) {
    return defaultValue;
  }

  @Override
  public R visitThis(@Nullable Object data, P params) {
    return defaultValue;
  }

  @Override
  public R visitLam(@Nullable Object data, @Nonnull Collection<? extends Abstract.Parameter> parameters, @Nullable Abstract.Expression body, P params) {
    return defaultValue;
  }

  @Override
  public R visitPi(@Nullable Object data, @Nonnull Collection<? extends Abstract.Parameter> parameters, @Nullable Abstract.Expression codomain, P params) {
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
  public R visitTuple(@Nullable Object data, @Nonnull Collection<? extends Abstract.Expression> fields, P params) {
    return defaultValue;
  }

  @Override
  public R visitSigma(@Nullable Object data, @Nonnull Collection<? extends Abstract.Parameter> parameters, P params) {
    return defaultValue;
  }

  @Override
  public R visitBinOpSequence(@Nullable Object data, @Nonnull Abstract.Expression left, @Nonnull Collection<? extends Abstract.BinOpSequenceElem> sequence, P params) {
    return defaultValue;
  }

  @Override
  public R visitCase(@Nullable Object data, boolean isSFunc, @Nullable Abstract.EvalKind evalKind, @Nonnull Collection<? extends Abstract.CaseArgument> caseArgs, @Nullable Abstract.Expression resultType, @Nullable Abstract.Expression resultTypeLevel, @Nonnull Collection<? extends Abstract.FunctionClause> clauses, P params) {
    return defaultValue;
  }

  @Override
  public R visitFieldAccs(@Nullable Object data, @Nonnull Abstract.Expression expression, @Nonnull Collection<Integer> fieldAccs, P params) {
    return defaultValue;
  }

  @Override
  public R visitClassExt(@Nullable Object data, boolean isNew, @Nullable Abstract.EvalKind evalKind, @Nullable Abstract.Expression baseClass, @Nullable Collection<? extends Abstract.ClassFieldImpl> implementations, @Nonnull Collection<? extends Abstract.BinOpSequenceElem> sequence, P params) {
    return defaultValue;
  }

  @Override
  public R visitLet(@Nullable Object data, boolean isStrict, @Nonnull Collection<? extends Abstract.LetClause> clauses, @Nullable Abstract.Expression expression, P params) {
    return defaultValue;
  }

  @Override
  public R visitNumericLiteral(@Nullable Object data, @Nonnull BigInteger number, P params) {
    return defaultValue;
  }

  @Override
  public R visitTyped(@Nullable Object data, @Nonnull Abstract.Expression expr, @Nonnull Abstract.Expression type, P params) {
    return defaultValue;
  }
}
