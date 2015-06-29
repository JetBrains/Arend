package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

public class FindDefCallVisitor implements ExpressionVisitor<Boolean> {
  private final Definition myDef;

  public FindDefCallVisitor(Definition def) {
    myDef = def;
  }

  @Override
  public Boolean visitApp(AppExpression expr) {
    return expr.getFunction().accept(this) || expr.getArgument().getExpression().accept(this);
  }

  @Override
  public Boolean visitDefCall(DefCallExpression expr) {
    return expr.getDefinition() == myDef;
  }

  @Override
  public Boolean visitIndex(IndexExpression expr) {
    return false;
  }

  @Override
  public Boolean visitLam(LamExpression expr) {
    for (Argument argument : expr.getArguments()) {
      if (argument instanceof TypeArgument) {
        if (((TypeArgument) argument).getType().accept(this)) return true;
      }
    }
    return expr.getBody().accept(this);
  }

  @Override
  public Boolean visitPi(PiExpression expr) {
    for (TypeArgument argument : expr.getArguments()) {
      if (argument.getType().accept(this)) return true;
    }
    return expr.getCodomain().accept(this);
  }

  @Override
  public Boolean visitUniverse(UniverseExpression expr) {
    return false;
  }

  @Override
  public Boolean visitVar(VarExpression expr) {
    return false;
  }

  @Override
  public Boolean visitInferHole(InferHoleExpression expr) {
    return false;
  }

  @Override
  public Boolean visitError(ErrorExpression expr) {
    return false;
  }

  @Override
  public Boolean visitTuple(TupleExpression expr) {
    for (Expression field : expr.getFields()) {
      if (field.accept(this)) return true;
    }
    return false;
  }

  @Override
  public Boolean visitSigma(SigmaExpression expr) {
    for (TypeArgument argument : expr.getArguments()) {
      if (argument.getType().accept(this)) return true;
    }
    return false;
  }

  @Override
  public Boolean visitElim(ElimExpression expr) {
    if (expr.getExpression().accept(this)) return true;
    for (Clause clause : expr.getClauses()) {
      if (clause == null) continue;
      for (Argument argument : clause.getArguments()) {
        if (argument instanceof TypeArgument) {
          if (((TypeArgument) argument).getType().accept(this)) return true;
        }
      }
      if (clause.getExpression().accept(this)) return true;
    }
    return expr.getOtherwise() == null ? false : expr.getOtherwise().getExpression().accept(this);
  }

  @Override
  public Boolean visitFieldAcc(FieldAccExpression expr) {
    return expr.getExpression().accept(this);
  }

  @Override
  public Boolean visitProj(ProjExpression expr) {
    return expr.getExpression().accept(this);
  }

  @Override
  public Boolean visitClassExt(ClassExtExpression expr) {
    // TODO
    return null;
  }
}
