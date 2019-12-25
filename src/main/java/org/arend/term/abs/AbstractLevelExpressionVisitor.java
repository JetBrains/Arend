package org.arend.term.abs;

import javax.annotation.Nullable;

public interface AbstractLevelExpressionVisitor<P, R> {
  R visitInf(@Nullable Object data, P param);
  R visitLP(@Nullable Object data, P param);
  R visitLH(@Nullable Object data, P param);
  R visitNumber(@Nullable Object data, int number, P param);
  R visitSuc(@Nullable Object data, /* @Nonnull */ @Nullable Abstract.LevelExpression expr, P param);
  R visitMax(@Nullable Object data, /* @Nonnull */ @Nullable Abstract.LevelExpression left, /* @Nonnull */ @Nullable Abstract.LevelExpression right, P param);
  R visitError(@Nullable Object data);
}
