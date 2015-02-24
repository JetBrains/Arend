package com.jetbrains.jetpad.term.visitor;

import com.jetbrains.jetpad.term.expr.*;

public interface ExpressionVisitor<T> {
    T visitApp(AppExpression expr);
    T visitDefCall(DefCallExpression expr);
    T visitIndex(IndexExpression expr);
    T visitLam(LamExpression expr);
    T visitNat(NatExpression expr);
    T visitNelim(NelimExpression expr);
    T visitPi(PiExpression expr);
    T visitSuc(SucExpression expr);
    T visitUniverse(UniverseExpression expr);
    T visitVar(VarExpression expr);
    T visitZero(ZeroExpression expr);
}
