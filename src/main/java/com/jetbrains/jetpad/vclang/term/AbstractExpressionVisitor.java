package com.jetbrains.jetpad.vclang.term;

public interface AbstractExpressionVisitor<P, R> {
  R visitApp(Abstract.AppExpression expr, P params);
  R visitReference(Abstract.ReferenceExpression expr, P params);
  R visitInferenceReference(Abstract.InferenceReferenceExpression expr, P params);
  R visitModuleCall(Abstract.ModuleCallExpression expr, P params);
  R visitLam(Abstract.LamExpression expr, P params);
  R visitPi(Abstract.PiExpression expr, P params);
  R visitUniverse(Abstract.UniverseExpression expr, P params);
  R visitInferHole(Abstract.InferHoleExpression expr, P params);
  R visitGoal(Abstract.GoalExpression expr, P params);
  R visitTuple(Abstract.TupleExpression expr, P params);
  R visitSigma(Abstract.SigmaExpression expr, P params);
  R visitBinOp(Abstract.BinOpExpression expr, P params);
  R visitBinOpSequence(Abstract.BinOpSequenceExpression expr, P params);
  R visitCase(Abstract.CaseExpression expr, P params);
  R visitProj(Abstract.ProjExpression expr, P params);
  R visitClassExt(Abstract.ClassExtExpression expr, P params);
  R visitNew(Abstract.NewExpression expr, P params);
  R visitLet(Abstract.LetExpression expr, P params);
  R visitNumericLiteral(Abstract.NumericLiteral expr, P params);
}
