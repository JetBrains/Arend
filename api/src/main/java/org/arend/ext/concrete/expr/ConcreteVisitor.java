package org.arend.ext.concrete.expr;

public interface ConcreteVisitor<P, R> {
  R visitApp(ConcreteAppExpression expr, P params);
  R visitReference(ConcreteReferenceExpression expr, P params);
  R visitThis(ConcreteThisExpression expr, P params);
  R visitLam(ConcreteLamExpression expr, P params);
  R visitPi(ConcretePiExpression expr, P params);
  R visitUniverse(ConcreteUniverseExpression expr, P params);
  R visitHole(ConcreteHoleExpression expr, P params);
  R visitGoal(ConcreteGoalExpression expr, P params);
  R visitTuple(ConcreteTupleExpression expr, P params);
  R visitSigma(ConcreteSigmaExpression expr, P params);
  R visitCase(ConcreteCaseExpression expr, P params);
  R visitEval(ConcreteEvalExpression expr, P params);
  R visitBox(ConcreteBoxExpression expr, P params);
  R visitProj(ConcreteProjExpression expr, P params);
  R visitClassExt(ConcreteClassExtExpression expr, P params);
  R visitNew(ConcreteNewExpression expr, P params);
  R visitLet(ConcreteLetExpression expr, P params);
  R visitNumber(ConcreteNumberExpression expr, P params);
  R visitString(ConcreteStringExpression expr, P params);
  R visitQName(ConcreteQNameExpression expr, P params);
  R visitTyped(ConcreteTypedExpression expr, P params);
  R visitCore(ConcreteCoreExpression expr, P params);
}
