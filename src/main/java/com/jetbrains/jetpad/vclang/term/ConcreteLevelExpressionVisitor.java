package com.jetbrains.jetpad.vclang.term;

public interface ConcreteLevelExpressionVisitor<T, P, R> {
  R visitInf(Concrete.InfLevelExpression<T> expr, P param);
  R visitLP(Concrete.PLevelExpression<T> expr, P param);
  R visitLH(Concrete.HLevelExpression<T> expr, P param);
  R visitNumber(Concrete.NumberLevelExpression<T> expr, P param);
  R visitSuc(Concrete.SucLevelExpression<T> expr, P param);
  R visitMax(Concrete.MaxLevelExpression<T> expr, P param);
  R visitVar(Concrete.InferVarLevelExpression<T> expr, P param);
}
