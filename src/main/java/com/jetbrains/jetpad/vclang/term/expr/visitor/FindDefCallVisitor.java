package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.OverriddenDefinition;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.List;

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
    if (expr.getDefinition() == myDef || expr.getExpression() != null && expr.getExpression().accept(this)) {
      return true;
    }
    if (expr.getParameters() == null) {
      return false;
    }
    for (Expression parameter : expr.getParameters()) {
      if (parameter.accept(this)) {
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

  @Override
  public Boolean visitClassExt(ClassExtExpression expr) {
    if (expr.getBaseClass() == myDef) return true;
    for (OverriddenDefinition definition : expr.getDefinitions()) {
      if (definition.getArguments() != null && visitArguments(definition.getArguments())) return true;
      if (definition.getResultType() != null && definition.getResultType().accept(this)) return true;
      if (definition.getTerm() != null && definition.getTerm().accept(this)) return true;
    }
    return false;
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
