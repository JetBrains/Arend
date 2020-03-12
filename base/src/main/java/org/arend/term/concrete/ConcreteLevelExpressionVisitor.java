package org.arend.term.concrete;

public interface ConcreteLevelExpressionVisitor<P, R> {
  R visitInf(Concrete.InfLevelExpression expr, P param);
  R visitLP(Concrete.PLevelExpression expr, P param);
  R visitLH(Concrete.HLevelExpression expr, P param);
  R visitNumber(Concrete.NumberLevelExpression expr, P param);
  R visitSuc(Concrete.SucLevelExpression expr, P param);
  R visitMax(Concrete.MaxLevelExpression expr, P param);
  R visitVar(Concrete.InferVarLevelExpression expr, P param);
}
