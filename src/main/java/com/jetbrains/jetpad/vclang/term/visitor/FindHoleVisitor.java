package com.jetbrains.jetpad.vclang.term.visitor;

import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

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
    CheckTypeVisitor.InferHoleExpression result = expr.getBody().accept(this);
    if (result != null) return result;
    for (Argument argument : expr.getArguments()) {
      if (argument instanceof TypeArgument) {
        result = ((TypeArgument) argument).getType().accept(this);
        if (result != null) return result;
      }
    }
    return null;
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
    CheckTypeVisitor.InferHoleExpression result = expr.getCodomain().accept(this);
    if (result != null) return result;
    for (TypeArgument argument : expr.getArguments()) {
      result = argument.getType().accept(this);
      if (result != null) return result;
    }
    return null;
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
