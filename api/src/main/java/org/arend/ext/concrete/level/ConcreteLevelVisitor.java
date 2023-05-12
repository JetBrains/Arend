package org.arend.ext.concrete.level;

public interface ConcreteLevelVisitor<P, R> {
  R visitInf(ConcreteInfLevel expr, P param);
  R visitLP(ConcreteLPLevel expr, P param);
  R visitLH(ConcreteLHLevel expr, P param);
  R visitNumber(ConcreteNumberLevel expr, P param);
  R visitVar(ConcreteVarLevel expr, P param);
  R visitSuc(ConcreteSucLevel expr, P param);
  R visitMax(ConcreteMaxLevel expr, P param);
}
