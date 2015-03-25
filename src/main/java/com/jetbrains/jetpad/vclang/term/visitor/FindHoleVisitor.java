package com.jetbrains.jetpad.vclang.term.visitor;

import com.jetbrains.jetpad.vclang.term.expr.*;

public class FindHoleVisitor implements ExpressionVisitor<CheckTypeVisitor.InferHoleExpression> {
  @Override
  public CheckTypeVisitor.InferHoleExpression visitApp(AppExpression expr) {
    CheckTypeVisitor.InferHoleExpression result = expr.getFunction().accept(this);
    return result == null ? expr.getArgument().accept(this) : result;
  }

  @Override
  public CheckTypeVisitor.InferHoleExpression visitDefCall(DefCallExpression expr) {
    return null;
  }

  @Override
  public CheckTypeVisitor.InferHoleExpression visitIndex(IndexExpression expr) {
    return null;
  }

  @Override
  public CheckTypeVisitor.InferHoleExpression visitLam(LamExpression expr) {
    return expr.getBody().accept(this);
  }

  @Override
  public CheckTypeVisitor.InferHoleExpression visitNat(NatExpression expr) {
    return null;
  }

  @Override
  public CheckTypeVisitor.InferHoleExpression visitNelim(NelimExpression expr) {
    return null;
  }

  @Override
  public CheckTypeVisitor.InferHoleExpression visitPi(PiExpression expr) {
    CheckTypeVisitor.InferHoleExpression result = expr.getDomain().accept(this);
    return result == null ? expr.getCodomain().accept(this) : result;
  }

  @Override
  public CheckTypeVisitor.InferHoleExpression visitSuc(SucExpression expr) {
    return null;
  }

  @Override
  public CheckTypeVisitor.InferHoleExpression visitUniverse(UniverseExpression expr) {
    return null;
  }

  @Override
  public CheckTypeVisitor.InferHoleExpression visitVar(VarExpression expr) {
    return null;
  }

  @Override
  public CheckTypeVisitor.InferHoleExpression visitZero(ZeroExpression expr) {
    return null;
  }

  @Override
  public CheckTypeVisitor.InferHoleExpression visitHole(HoleExpression expr) {
    return expr instanceof CheckTypeVisitor.InferHoleExpression ? (CheckTypeVisitor.InferHoleExpression) expr : null;
  }
}
