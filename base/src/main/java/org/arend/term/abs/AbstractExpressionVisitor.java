package org.arend.term.abs;

import org.arend.naming.reference.Referable;
import org.arend.term.Fixity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.Collection;

public interface AbstractExpressionVisitor<P, R> {
  R visitReference(@Nullable Object data, @NotNull Referable referent, @Nullable Fixity fixity, @Nullable Abstract.LevelExpression level1, @Nullable Abstract.LevelExpression level2, P params);
  R visitReference(@Nullable Object data, @NotNull Referable referent, int lp, int lh, P params);
  R visitThis(@Nullable Object data, P params);
  R visitLam(@Nullable Object data, @NotNull Collection<? extends Abstract.Parameter> parameters, /* @NotNull */ @Nullable Abstract.Expression body, P params);
  R visitPi(@Nullable Object data, @NotNull Collection<? extends Abstract.Parameter> parameters, /* @NotNull */ @Nullable Abstract.Expression codomain, P params);
  R visitUniverse(@Nullable Object data, @Nullable Integer pLevelNum, @Nullable Integer hLevelNum, @Nullable Abstract.LevelExpression pLevel, @Nullable Abstract.LevelExpression hLevel, P params);
  R visitApplyHole(@Nullable Object data, P params);
  R visitInferHole(@Nullable Object data, P params);
  R visitGoal(@Nullable Object data, @Nullable String name, @Nullable Abstract.Expression expression, P params);
  R visitTuple(@Nullable Object data, @NotNull Collection<? extends Abstract.Expression> fields, P params);
  R visitSigma(@Nullable Object data, @NotNull Collection<? extends Abstract.Parameter> parameters, P params);
  R visitBinOpSequence(@Nullable Object data, @NotNull Abstract.Expression left, @NotNull Collection<? extends Abstract.BinOpSequenceElem> sequence, P params);
  R visitCase(@Nullable Object data, boolean isSFunc, @Nullable Abstract.EvalKind evalKind, @NotNull Collection<? extends Abstract.CaseArgument> arguments, @Nullable Abstract.Expression resultType, @Nullable Abstract.Expression resultTypeLevel, @NotNull Collection<? extends Abstract.FunctionClause> clauses, P params);
  R visitFieldAccs(@Nullable Object data, @NotNull Abstract.Expression expression, @NotNull Collection<Integer> fieldAccs, P params);
  R visitClassExt(@Nullable Object data, boolean isNew, @Nullable Abstract.EvalKind evalKind, /* @NotNull */ @Nullable Abstract.Expression baseClass, @Nullable Collection<? extends Abstract.ClassFieldImpl> implementations, @NotNull Collection<? extends Abstract.BinOpSequenceElem> sequence, P params);
  R visitLet(@Nullable Object data, boolean isStrict, @NotNull Collection<? extends Abstract.LetClause> clauses, /* @NotNull */ @Nullable Abstract.Expression expression, P params);
  R visitNumericLiteral(@Nullable Object data, @NotNull BigInteger number, P params);
  R visitTyped(@Nullable Object data, @NotNull Abstract.Expression expr, @NotNull Abstract.Expression type, P params);
}
