package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

public class FindHoleVisitor implements ExpressionVisitor<InferHoleExpression> {
  @Override
  public InferHoleExpression visitApp(AppExpression expr) {
    InferHoleExpression result = expr.getFunction().accept(this);
    return result == null ? expr.getArgument().accept(this) : result;
  }

  @Override
  public InferHoleExpression visitDefCall(DefCallExpression expr) {
    return null;
  }

  @Override
  public InferHoleExpression visitIndex(IndexExpression expr) {
    return null;
  }

  @Override
  public InferHoleExpression visitLam(LamExpression expr) {
    InferHoleExpression result = expr.getBody().accept(this);
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
  public InferHoleExpression visitNat(NatExpression expr) {
    return null;
  }

  @Override
  public InferHoleExpression visitNelim(NelimExpression expr) {
    return null;
  }

  @Override
  public InferHoleExpression visitPi(PiExpression expr) {
    InferHoleExpression result = expr.getCodomain().accept(this);
    if (result != null) return result;
    for (TypeArgument argument : expr.getArguments()) {
      result = argument.getType().accept(this);
      if (result != null) return result;
    }
    return null;
  }

  @Override
  public InferHoleExpression visitSuc(SucExpression expr) {
    return null;
  }

  @Override
  public InferHoleExpression visitUniverse(UniverseExpression expr) {
    return null;
  }

  @Override
  public InferHoleExpression visitVar(VarExpression expr) {
    return null;
  }

  @Override
  public InferHoleExpression visitZero(ZeroExpression expr) {
    return null;
  }

  @Override
  public InferHoleExpression visitError(ErrorExpression expr) {
    return null;
  }

  @Override
  public InferHoleExpression visitInferHole(InferHoleExpression expr) {
    return expr;
  }

  @Override
  public InferHoleExpression visitTuple(TupleExpression expr) {
    for (Expression field : expr.getFields()) {
      InferHoleExpression result = field.accept(this);
      if (result != null) return result;
    }
    return null;
  }

  @Override
  public InferHoleExpression visitSigma(SigmaExpression expr) {
    for (TypeArgument argument : expr.getArguments()) {
      InferHoleExpression result = argument.getType().accept(this);
      if (result != null) return result;
    }
    return null;
  }

  @Override
  public InferHoleExpression visitBinOp(BinOpExpression expr) {
    InferHoleExpression result = expr.getLeft().accept(this);
    return result == null ? expr.getRight().accept(this) : result;
  }

  @Override
  public InferHoleExpression visitElim(ElimExpression expr) {
    InferHoleExpression result = expr.getExpression().accept(this);
    if (result != null) return result;
    for (Clause clause : expr.getClauses()) {
      result = clause.getExpression().accept(this);
      if (result != null) return result;
      for (Argument argument : clause.getArguments()) {
        if (argument instanceof TypeArgument) {
          result = ((TypeArgument) argument).getType().accept(this);
          if (result != null) return result;
        }
      }
    }
    return null;
  }
}
