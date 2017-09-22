package com.jetbrains.jetpad.vclang.frontend.term;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Collection;

public interface AbstractExpressionVisitor<P, R> {
  R visitApp(@Nullable Object data, @Nonnull Abstract.Expression expr, @Nonnull Collection<? extends Abstract.Argument> arguments, P params);
  R visitReference(@Nullable Object data, @Nullable Abstract.Expression expr, @Nonnull Referable referent, P params);
  R visitModuleCall(@Nullable Object data, @Nonnull ModulePath modulePath, P params);
  R visitLam(@Nullable Object data, @Nonnull Collection<? extends Abstract.Parameter> parameters, @Nonnull Abstract.Expression body, P params);
  R visitPi(@Nullable Object data, @Nonnull Collection<? extends Abstract.Parameter> parameters, @Nonnull Abstract.Expression codomain, P params);
  R visitUniverse(@Nullable Object data, @Nonnull Abstract.LevelExpression pLevel, @Nonnull Abstract.LevelExpression hLevel, P params);
  R visitInferHole(@Nullable Object data, P params);
  R visitGoal(@Nullable Object data, @Nullable String name, @Nullable Abstract.Expression expression, P params);
  R visitTuple(@Nullable Object data, @Nonnull Collection<? extends Abstract.Expression> fields, P params);
  R visitSigma(@Nullable Object data, @Nonnull Collection<? extends Abstract.Parameter> parameters, P params);
  R visitBinOp(@Nullable Object data, @Nonnull Abstract.Expression left, @Nonnull Referable binOp, @Nullable Abstract.Expression right, P params);
  R visitBinOpSequence(@Nullable Object data, @Nonnull Abstract.Expression left, @Nonnull Collection<? extends Abstract.BinOpSequenceElem> sequence, P params);
  R visitCase(@Nullable Object data, @Nonnull Collection<? extends Abstract.Expression> expressions, @Nonnull Collection<? extends Abstract.FunctionClause> clauses, P params);
  R visitProj(@Nullable Object data, @Nonnull Abstract.Expression expression, int field, P params);
  R visitClassExt(@Nullable Object data, @Nonnull Abstract.Expression baseClass, @Nonnull Collection<? extends Abstract.ClassFieldImpl> implementations, P params);
  R visitNew(@Nullable Object data, @Nonnull Abstract.Expression expression, P params);
  R visitLet(@Nullable Object data, @Nonnull Collection<? extends Abstract.LetClause> clauses, @Nonnull Abstract.Expression expression, P params);
  R visitNumericLiteral(@Nullable Object data, @Nonnull BigInteger number, P params);
}
