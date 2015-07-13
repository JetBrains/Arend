package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.definition.OverriddenDefinition;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

public class FindHoleVisitor implements ExpressionVisitor<InferHoleExpression> {
  @Override
  public InferHoleExpression visitApp(AppExpression expr) {
    InferHoleExpression result = expr.getFunction().accept(this);
    return result == null ? expr.getArgument().getExpression().accept(this) : result;
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
  public InferHoleExpression visitUniverse(UniverseExpression expr) {
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
  public InferHoleExpression visitElim(ElimExpression expr) {
    InferHoleExpression result = expr.getExpression().accept(this);
    if (result != null) return result;
    for (Clause clause : expr.getClauses()) {
      if (clause == null) continue;
      result = clause.getExpression().accept(this);
      if (result != null) return result;
      for (Argument argument : clause.getArguments()) {
        if (argument instanceof TypeArgument) {
          result = ((TypeArgument) argument).getType().accept(this);
          if (result != null) return result;
        }
      }
    }
    return expr.getOtherwise() == null ? null : expr.getOtherwise().getExpression().accept(this);
  }

  @Override
  public InferHoleExpression visitFieldAcc(FieldAccExpression expr) {
    return expr.getExpression().accept(this);
  }

  @Override
  public InferHoleExpression visitProj(ProjExpression expr) {
    return expr.getExpression().accept(this);
  }

  @Override
  public InferHoleExpression visitClassExt(ClassExtExpression expr) {
    for (OverriddenDefinition definition : expr.getDefinitions()) {
      if (definition.getArguments() != null) {
        for (Argument argument : definition.getArguments()) {
          if (argument instanceof TypeArgument) {
            InferHoleExpression result = ((TypeArgument) argument).getType().accept(this);
            if (result != null) return result;
          }
        }
      }
      if (definition.getResultType() != null) {
        InferHoleExpression result = definition.getResultType().accept(this);
        if (result != null) return result;
      }
      if (definition.getTerm() != null) {
        InferHoleExpression result = definition.getTerm().accept(this);
        if (result != null) return result;
      }
    }
    return null;
  }

  @Override
  public InferHoleExpression visitNew(NewExpression expr) {
    return expr.getExpression().accept(this);
  }
}
