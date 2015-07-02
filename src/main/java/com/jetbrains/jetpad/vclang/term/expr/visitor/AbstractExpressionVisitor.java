package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;

public interface AbstractExpressionVisitor<P, R> {
  R visitApp(Abstract.AppExpression expr, P params);
  R visitDefCall(Abstract.DefCallExpression expr, P params);
  R visitIndex(Abstract.IndexExpression expr, P params);
  R visitLam(Abstract.LamExpression expr, P params);
  R visitPi(Abstract.PiExpression expr, P params);
  R visitUniverse(Abstract.UniverseExpression expr, P params);
  R visitVar(Abstract.VarExpression expr, P params);
  R visitInferHole(Abstract.InferHoleExpression expr, P params);
  R visitError(Abstract.ErrorExpression expr, P params);
  R visitTuple(Abstract.TupleExpression expr, P params);
  R visitSigma(Abstract.SigmaExpression expr, P params);
  R visitBinOp(Abstract.BinOpExpression expr, P params);
  R visitElim(Abstract.ElimExpression expr, P params);
  R visitFieldAcc(Abstract.FieldAccExpression expr, P params);
  R visitProj(Abstract.ProjExpression expr, P params);
  R visitClassExt(Abstract.ClassExtExpression expr, P params);
  R visitNew(Abstract.NewExpression expr, P params);
}
