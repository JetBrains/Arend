package com.jetbrains.jetpad.vclang.term;

public interface ConcreteExpressionVisitor<P, R> {
  R visitApp(Concrete.AppExpression expr, P params);
  R visitReference(Concrete.ReferenceExpression expr, P params);
  R visitInferenceReference(Concrete.InferenceReferenceExpression expr, P params);
  R visitModuleCall(Concrete.ModuleCallExpression expr, P params);
  R visitLam(Concrete.LamExpression expr, P params);
  R visitPi(Concrete.PiExpression expr, P params);
  R visitUniverse(Concrete.UniverseExpression expr, P params);
  R visitInferHole(Concrete.InferHoleExpression expr, P params);
  R visitGoal(Concrete.GoalExpression expr, P params);
  R visitTuple(Concrete.TupleExpression expr, P params);
  R visitSigma(Concrete.SigmaExpression expr, P params);
  R visitBinOp(Concrete.BinOpExpression expr, P params);
  R visitBinOpSequence(Concrete.BinOpSequenceExpression expr, P params);
  R visitCase(Concrete.CaseExpression expr, P params);
  R visitProj(Concrete.ProjExpression expr, P params);
  R visitClassExt(Concrete.ClassExtExpression expr, P params);
  R visitNew(Concrete.NewExpression expr, P params);
  R visitLet(Concrete.LetExpression expr, P params);
  R visitNumericLiteral(Concrete.NumericLiteral expr, P params);
}
