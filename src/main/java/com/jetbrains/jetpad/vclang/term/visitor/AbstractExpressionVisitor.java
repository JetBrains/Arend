package com.jetbrains.jetpad.vclang.term.visitor;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;

public interface AbstractExpressionVisitor<T> {
  T visitApp(Abstract.AppExpression expr);
  T visitDefCall(Abstract.DefCallExpression expr);
  T visitIndex(Abstract.IndexExpression expr);
  T visitLam(Abstract.LamExpression expr);
  T visitNat(Abstract.NatExpression expr);
  T visitNelim(Abstract.NelimExpression expr);
  T visitPi(Abstract.PiExpression expr);
  T visitSuc(Abstract.SucExpression expr);
  T visitUniverse(Abstract.UniverseExpression expr);
  T visitVar(Abstract.VarExpression expr);
  T visitZero(Abstract.ZeroExpression expr);
}
