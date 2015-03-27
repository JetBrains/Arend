package com.jetbrains.jetpad.vclang.term.visitor;

import com.jetbrains.jetpad.vclang.term.expr.*;

import static com.jetbrains.jetpad.vclang.term.expr.Expression.*;

public class LiftIndexVisitor implements ExpressionVisitor<Expression> {
  private final int from;
  private final int on;

  public LiftIndexVisitor(int from, int on) {
    this.from = from;
    this.on = on;
  }

  @Override
  public Expression visitApp(AppExpression expr) {
    return Apps(expr.getFunction().accept(this), expr.getArgument().accept(this));
  }

  @Override
  public Expression visitDefCall(DefCallExpression expr) {
    return expr;
  }

  @Override
  public Expression visitIndex(IndexExpression expr) {
    if (expr.getIndex() < from) return expr;
    if (expr.getIndex() + on >= 0) return Index(expr.getIndex() + on);
    throw new NegativeIndex();
  }

  @Override
  public Expression visitLam(LamExpression expr) {
    return Lam(expr.getVariable(), expr.getBody().liftIndex(from + 1, on));
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
    return Pi(expr.isExplicit(), expr.getVariable(), expr.getDomain().accept(this), expr.getCodomain().liftIndex(from + 1, on));
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

  @Override
  public Expression visitHole(HoleExpression expr) {
    return expr.getInstance(expr.expression() == null ? null : expr.expression().accept(this));
  }

  public static class NegativeIndex extends RuntimeException {}
}
