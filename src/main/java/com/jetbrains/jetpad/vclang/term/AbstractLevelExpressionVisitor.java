package com.jetbrains.jetpad.vclang.term;

public interface AbstractLevelExpressionVisitor<P, R> {
  R visitInf(Abstract.InfLevelExpression expr, P param);
  R visitLP(Abstract.PLevelExpression expr, P param);
  R visitLH(Abstract.HLevelExpression expr, P param);
  R visitNumber(Abstract.NumberLevelExpression expr, P param);
  R visitSuc(Abstract.SucLevelExpression expr, P param);
  R visitMax(Abstract.MaxLevelExpression expr, P param);
  R visitVar(Abstract.InferVarLevelExpression expr, P param);
}
