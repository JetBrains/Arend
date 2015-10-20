package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.expr.*;

public interface ExpressionVisitor<T> {
  T visitApp(AppExpression expr);
  T visitFunCall(FunCallExpression expr);
  T visitConCall(ConCallExpression expr);
  T visitDataCall(DataCallExpression expr);
  T visitFieldCall(FieldCallExpression expr);
  T visitClassCall(ClassCallExpression expr);
  T visitIndex(IndexExpression expr);
  T visitLam(LamExpression expr);
  T visitPi(PiExpression expr);
  T visitUniverse(UniverseExpression expr);
  T visitInferHole(InferHoleExpression expr);
  T visitError(ErrorExpression expr);
  T visitTuple(TupleExpression expr);
  T visitSigma(SigmaExpression expr);
  T visitElim(ElimExpression expr);
  T visitProj(ProjExpression expr);
  T visitClassExt(ClassExtExpression expr);
  T visitNew(NewExpression expr);
  T visitLet(LetExpression letExpression);
}
