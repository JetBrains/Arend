package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.List;
import java.util.Map;

public class FindHoleVisitor extends BaseExpressionVisitor<InferHoleExpression> {
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
  public InferHoleExpression visitConCall(ConCallExpression expr) {
    for (Expression parameter : expr.getParameters()) {
      InferHoleExpression result = parameter.accept(this);
      if (result != null) return result;
    }
    return null;
  }

  @Override
  public InferHoleExpression visitClassCall(ClassCallExpression expr) {
    for (Map.Entry<ClassField, ClassCallExpression.OverrideElem> elem : expr.getOverrideElems().entrySet()) {
      Expression type = elem.getValue().type;
      if (type != null) {
        InferHoleExpression result = type.accept(this);
        if (result != null) return result;
      }
      
      Expression term = elem.getValue().term;
      if (term != null) {
        InferHoleExpression result = term.accept(this);
        if (result != null) return result;
      }
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
