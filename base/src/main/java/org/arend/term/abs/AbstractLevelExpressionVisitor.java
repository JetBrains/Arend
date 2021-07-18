package org.arend.term.abs;

import org.arend.naming.reference.Referable;
import org.jetbrains.annotations.Nullable;

public interface AbstractLevelExpressionVisitor<P, R> {
  R visitInf(@Nullable Object data, P param);
  R visitLP(@Nullable Object data, P param);
  R visitLH(@Nullable Object data, P param);
  R visitNumber(@Nullable Object data, int number, P param);
  R visitId(@Nullable Object data, Referable ref, P param);
  R visitSuc(@Nullable Object data, /* @NotNull */ @Nullable Abstract.LevelExpression expr, P param);
  R visitMax(@Nullable Object data, /* @NotNull */ @Nullable Abstract.LevelExpression left, /* @NotNull */ @Nullable Abstract.LevelExpression right, P param);
  R visitError(@Nullable Object data, P param);
}
