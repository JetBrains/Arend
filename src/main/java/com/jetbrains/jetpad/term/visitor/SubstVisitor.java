package com.jetbrains.jetpad.term.visitor;

import com.jetbrains.jetpad.term.expr.*;

public class SubstVisitor implements ExpressionVisitor<Expression> {
    private final Expression substExpr;
    private final int from;

    public SubstVisitor(Expression substExpr, int from) {
        this.substExpr = substExpr;
        this.from = from;
    }

    @Override
    public Expression visitApp(AppExpression expr) {
        return new AppExpression(expr.getFunction().accept(this), expr.getArgument().accept(this));
    }

    @Override
    public Expression visitDefCall(DefCallExpression expr) {
        return expr;
    }

    @Override
    public Expression visitIndex(IndexExpression expr) {
        if (expr.getIndex() < from) return expr;
        if (expr.getIndex() == from) return substExpr; // .liftIndex(0, from);
        return new IndexExpression(expr.getIndex() - 1);
    }

    @Override
    public Expression visitLam(LamExpression expr) {
        return new LamExpression(expr.getVariable(), expr.getBody().subst(substExpr.liftIndex(0, 1), from + 1));
    }

    @Override
    public Expression visitNat(NatExpression expr) {
        return expr;
    }

    @Override
    public Expression visitNelim(NelimExpression expr) {
        return expr;
    }

    @Override
    public Expression visitPi(PiExpression expr) {
        return new PiExpression(expr.isExplicit(), expr.getVariable(), expr.getLeft().accept(this), expr.getRight().subst(substExpr, from + 1));
    }

    @Override
    public Expression visitSuc(SucExpression expr) {
        return expr;
    }

    @Override
    public Expression visitUniverse(UniverseExpression expr) {
        return expr;
    }

    @Override
    public Expression visitVar(VarExpression expr) {
        return expr;
    }

    @Override
    public Expression visitZero(ZeroExpression expr) {
        return expr;
    }
}
