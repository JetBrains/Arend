package com.jetbrains.jetpad.vclang.term.visitor;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;

public interface AbstractExpressionVisitor<P, R> {
  R visitApp(Abstract.AppExpression expr, P params);
  R visitDefCall(Abstract.DefCallExpression expr, P params);
  R visitIndex(Abstract.IndexExpression expr, P params);
  R visitLam(Abstract.LamExpression expr, P params);
  R visitNat(Abstract.NatExpression expr, P params);
  R visitNelim(Abstract.NelimExpression expr, P params);
  R visitPi(Abstract.PiExpression expr, P params);
  R visitSuc(Abstract.SucExpression expr, P params);
  R visitUniverse(Abstract.UniverseExpression expr, P params);
  R visitVar(Abstract.VarExpression expr, P params);
  R visitZero(Abstract.ZeroExpression expr, P params);
  R visitInferHole(Abstract.InferHoleExpression expr, P params);
  R visitError(Abstract.ErrorExpression expr, P params);
  R visitTuple(Abstract.TupleExpression expr, P params);
  R visitSigma(Abstract.SigmaExpression expr, P params);
  R visitBinOp(Abstract.BinOpExpression expr, P params);
}
