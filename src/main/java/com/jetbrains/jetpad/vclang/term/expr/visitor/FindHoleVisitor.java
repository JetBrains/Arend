package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.List;
import java.util.Map;

public class FindHoleVisitor extends BaseExpressionVisitor<Void, InferHoleExpression> {
  @Override
  public InferHoleExpression visitApp(AppExpression expr, Void params) {
    InferHoleExpression result = expr.getFunction().accept(this, null);
    return result == null ? expr.getArgument().getExpression().accept(this, null) : result;
  }

  @Override
  public InferHoleExpression visitDefCall(DefCallExpression expr, Void params) {
    return null;
  }

  @Override
  public InferHoleExpression visitConCall(ConCallExpression expr, Void params) {
    for (Expression parameter : expr.getParameters()) {
      InferHoleExpression result = parameter.accept(this, null);
      if (result != null) return result;
    }
    return null;
  }

  @Override
  public InferHoleExpression visitClassCall(ClassCallExpression expr, Void params) {
    for (Map.Entry<ClassField, ClassCallExpression.ImplementStatement> elem : expr.getImplementStatements().entrySet()) {
      Expression type = elem.getValue().type;
      if (type != null) {
        InferHoleExpression result = type.accept(this, null);
        if (result != null) return result;
      }
      
      Expression term = elem.getValue().term;
      if (term != null) {
        InferHoleExpression result = term.accept(this, null);
        if (result != null) return result;
      }
    }
    return null;
  }

  @Override
  public InferHoleExpression visitIndex(IndexExpression expr, Void params) {
    return null;
  }

  @Override
  public InferHoleExpression visitLam(LamExpression expr, Void params) {
    InferHoleExpression result = expr.getBody().accept(this, null);
    if (result != null) return result;
    return visitArguments(expr.getArguments());
  }

  @Override
  public InferHoleExpression visitPi(PiExpression expr, Void params) {
    InferHoleExpression result = expr.getCodomain().accept(this, null);
    if (result != null) return result;
    return visitArguments(expr.getArguments());
  }

  @Override
  public InferHoleExpression visitUniverse(UniverseExpression expr, Void params) {
    return null;
  }

  @Override
  public InferHoleExpression visitError(ErrorExpression expr, Void params) {
    return null;
  }

  @Override
  public InferHoleExpression visitInferHole(InferHoleExpression expr, Void params) {
    return expr;
  }

  @Override
  public InferHoleExpression visitTuple(TupleExpression expr, Void params) {
    for (Expression field : expr.getFields()) {
      InferHoleExpression result = field.accept(this, null);
      if (result != null) return result;
    }
    return null;
  }

  @Override
  public InferHoleExpression visitSigma(SigmaExpression expr, Void params) {
    return visitArguments(expr.getArguments());
  }

  @Override
  public InferHoleExpression visitElim(ElimExpression expr, Void params) {
    for (Clause clause : expr.getClauses()) {
      InferHoleExpression result = clause.getExpression().accept(this, null);
      if (result != null) return result;
    }
    return null;
  }

  private InferHoleExpression visitArguments(List<? extends Argument> arguments) {
    InferHoleExpression result;
    for (Argument argument : arguments) {
      if (argument instanceof TypeArgument) {
        result = ((TypeArgument) argument).getType().accept(this, null);
        if (result != null) return result;
      }
    }
    return null;
  }

  @Override
  public InferHoleExpression visitProj(ProjExpression expr, Void params) {
    return expr.getExpression().accept(this, null);
  }

  @Override
  public InferHoleExpression visitNew(NewExpression expr, Void params) {
    return expr.getExpression().accept(this, null);
  }

  @Override
  public InferHoleExpression visitLet(LetExpression letExpression, Void params) {
    InferHoleExpression result;
    for (LetClause letClause : letExpression.getClauses()) {
      result = visitLetClause(letClause);
      if (result != null) return result;
    }
    return letExpression.getExpression().accept(this, null);
  }

  private InferHoleExpression visitLetClause(LetClause clause) {
    InferHoleExpression result = visitArguments(clause.getArguments());
    if (result != null) return result;
    if (clause.getType() != null) result = clause.getType().accept(this, null);
    if (result != null) return result;
    return clause.getTerm().accept(this, null);
  }
}
