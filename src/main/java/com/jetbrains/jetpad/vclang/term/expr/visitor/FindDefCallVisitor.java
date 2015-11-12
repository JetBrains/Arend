package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.List;
import java.util.Map;

public class FindDefCallVisitor extends BaseExpressionVisitor<Boolean> {
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
  public Boolean visitConCall(ConCallExpression expr) {
    if (expr.getDefinition() == myDef) {
      return true;
    }
    for (Expression parameter : expr.getParameters()) {
      if (parameter.accept(this)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Boolean visitClassCall(ClassCallExpression expr) {
    if (expr.getDefinition() == myDef) {
      return true;
    }
    for (Map.Entry<ClassField, ClassCallExpression.ImplementStatement> elem : expr.getImplementStatements().entrySet()) {
      if (elem.getKey() == myDef || elem.getValue().type != null && elem.getValue().type.accept(this) || elem.getValue().term != null && elem.getValue().term.accept(this)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Boolean visitIndex(IndexExpression expr) {
    return false;
  }

  @Override
  public Boolean visitLam(LamExpression expr) {
    if (visitArguments(expr.getArguments())) return true;
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
    for (Clause clause : expr.getClauses()) {
      if (clause.getExpression().accept(this)) return true;
    }
    return false;
  }

  @Override
  public Boolean visitProj(ProjExpression expr) {
    return expr.getExpression().accept(this);
  }

  private boolean visitArguments(List<? extends Argument> arguments) {
    for (Argument argument : arguments) {
      if (argument instanceof TypeArgument && ((TypeArgument) argument).getType().accept(this)) return true;
    }
    return false;
  }

  @Override
  public Boolean visitNew(NewExpression expr) {
    return expr.getExpression().accept(this);
  }

  @Override
  public Boolean visitLet(LetExpression letExpression) {
    for (LetClause clause : letExpression.getClauses()) {
      if (visitLetClause(clause)) return true;
    }
    return letExpression.getExpression().accept(this);
  }

  public boolean visitLetClause(LetClause clause) {
    if (visitArguments(clause.getArguments())) return true;
    if (clause.getResultType() != null && clause.getResultType().accept(this)) return true;
    return clause.getTerm().accept(this);
  }
}
