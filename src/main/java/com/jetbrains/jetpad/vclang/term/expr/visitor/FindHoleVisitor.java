package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.definition.OverriddenDefinition;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.List;

public class FindHoleVisitor implements ExpressionVisitor<InferHoleExpression> {
  @Override
  public InferHoleExpression visitApp(AppExpression expr) {
    InferHoleExpression result = expr.getFunction().accept(this);
    return result == null ? expr.getArgument().getExpression().accept(this) : result;
  }

  @Override
  public InferHoleExpression visitDefCall(DefCallExpression expr) {
    InferHoleExpression result = expr.getExpression() == null ? null : expr.getExpression().accept(this);
    if (result != null) return result;
    if (expr.getParameters() == null) return null;
    for (Expression parameter : expr.getParameters()) {
      result = parameter.accept(this);
      if (result != null) return result;
    }
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
    return visitArguments(expr.getArguments());
  }
  @Override
  public InferHoleExpression visitPi(PiExpression expr) {
    InferHoleExpression result = expr.getCodomain().accept(this);
    if (result != null) return result;
    return visitArguments(expr.getArguments());
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
    return visitArguments(expr.getArguments());
  }

  @Override
  public InferHoleExpression visitElim(ElimExpression expr) {
    for (Clause clause : expr.getClauses()) {
      InferHoleExpression result = clause.getExpression().accept(this);
      if (result != null) return result;
    }
    return null;
  }

  private InferHoleExpression visitArguments(List<? extends Argument> arguments) {
    InferHoleExpression result;
    for (Argument argument : arguments) {
      if (argument instanceof TypeArgument) {
        result = ((TypeArgument) argument).getType().accept(this);
        if (result != null) return result;
      }
    }
    return null;
  }

  @Override
  public InferHoleExpression visitProj(ProjExpression expr) {
    return expr.getExpression().accept(this);
  }

  @Override
  public InferHoleExpression visitClassExt(ClassExtExpression expr) {
    for (OverriddenDefinition definition : expr.getDefinitions()) {
      if (definition.getArguments() != null) {
        InferHoleExpression result = visitArguments(definition.getArguments());
        if (result != null) return result;
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

  @Override
  public InferHoleExpression visitLet(LetExpression letExpression) {
    InferHoleExpression result;
    for (LetClause letClause : letExpression.getClauses()) {
      result = visitLetClause(letClause);
      if (result != null) return result;
    }
    return letExpression.getExpression().accept(this);
  }

  private InferHoleExpression visitLetClause(LetClause clause) {
    InferHoleExpression result = visitArguments(clause.getArguments());
    if (result != null) return result;
    if (clause.getType() != null) result = clause.getType().accept(this);
    if (result != null) return result;
    return clause.getTerm().accept(this);
  }
}
