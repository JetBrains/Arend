package com.jetbrains.jetpad.vclang.frontend.term;

import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;

import javax.annotation.Nullable;

public interface AbstractLevelExpressionVisitor<P, R> {
  R visitInf(@Nullable Object data, P param);
  R visitLP(@Nullable Object data, P param);
  R visitLH(@Nullable Object data, P param);
  R visitNumber(@Nullable Object data, int number, P param);
  R visitSuc(@Nullable Object data, Abstract.LevelExpression expr, P param);
  R visitMax(@Nullable Object data, Abstract.LevelExpression left, Abstract.LevelExpression right, P param);
  R visitVar(@Nullable Object data, InferenceLevelVariable var, P param);
}
