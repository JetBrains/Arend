package com.jetbrains.jetpad.vclang.term.visitor;

import com.jetbrains.jetpad.vclang.term.expr.*;

public class LiftIndexVisitor implements ExpressionVisitor<Expression> {
  private final int from;
  private final int on;

  public LiftIndexVisitor(int from, int on) {
    this.from = from;
    this.on = on;
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
    if (expr.getIndex() < from) {
      return expr;
    } else {
      return new IndexExpression(expr.getIndex() + on);
    }
  }

  @Override
  public Expression visitLam(LamExpression expr) {
    return new LamExpression(expr.getVariable(), expr.getBody().liftIndex(from + 1, on));
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
    return new PiExpression(expr.isExplicit(), expr.getVariable(), expr.getLeft().accept(this), expr.getRight().liftIndex(from + 1, on));
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
