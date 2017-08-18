package com.jetbrains.jetpad.vclang.term;

public interface ConcreteExpressionVisitor<T, P, R> {
  R visitApp(Concrete.AppExpression<T> expr, P params);
  R visitReference(Concrete.ReferenceExpression<T> expr, P params);
  R visitInferenceReference(Concrete.InferenceReferenceExpression<T> expr, P params);
  R visitModuleCall(Concrete.ModuleCallExpression<T> expr, P params);
  R visitLam(Concrete.LamExpression<T> expr, P params);
  R visitPi(Concrete.PiExpression<T> expr, P params);
  R visitUniverse(Concrete.UniverseExpression<T> expr, P params);
  R visitInferHole(Concrete.InferHoleExpression<T> expr, P params);
  R visitGoal(Concrete.GoalExpression<T> expr, P params);
  R visitTuple(Concrete.TupleExpression<T> expr, P params);
  R visitSigma(Concrete.SigmaExpression<T> expr, P params);
  R visitBinOp(Concrete.BinOpExpression<T> expr, P params);
  R visitBinOpSequence(Concrete.BinOpSequenceExpression<T> expr, P params);
  R visitCase(Concrete.CaseExpression<T> expr, P params);
  R visitProj(Concrete.ProjExpression<T> expr, P params);
  R visitClassExt(Concrete.ClassExtExpression<T> expr, P params);
  R visitNew(Concrete.NewExpression<T> expr, P params);
  R visitLet(Concrete.LetExpression<T> expr, P params);
  R visitNumericLiteral(Concrete.NumericLiteral<T> expr, P params);
}
