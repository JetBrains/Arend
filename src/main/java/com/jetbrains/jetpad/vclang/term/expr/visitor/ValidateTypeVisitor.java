package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.*;

import java.util.ArrayList;

public class ValidateTypeVisitor extends BaseExpressionVisitor<Void, Void> {

  public static class ErrorReporter {
    private final ArrayList<Expression> expressions = new ArrayList<>();
    private final ArrayList<String> reasons = new ArrayList<>();

    public void addError(Expression expr, String reason) {
      expressions.add(expr);
      reasons.add(reason);
    }

    public ArrayList<Expression> getExpressions() {
      return expressions;
    }

    public ArrayList<String> getReasons() {
      return reasons;
    }

    public int errors() {
      return expressions.size();
    }
  }

  private final ErrorReporter myErrorReporter;

  public ValidateTypeVisitor(ErrorReporter errorReporter) {
    this.myErrorReporter = errorReporter;
  }

  @Override
  public Void visitDefCall(DefCallExpression expr, Void params) {
    expr.getType().accept(this, params);
    return null;
  }

  @Override
  public Void visitApp(AppExpression expr, Void params) {
    Expression fun = expr.getFunction();
    Expression funType = fun.getType();
    if (!(funType instanceof PiExpression)) {
      myErrorReporter.addError(expr, "Function " + fun + " doesn't have Pi-type");
    }
    return null;
  }

  @Override
  public Void visitReference(ReferenceExpression expr, Void params) {
    expr.getBinding().getType().accept(this, params);
    return null;
  }

  @Override
  public Void visitLam(LamExpression expr, Void params) {
    expr.getType().accept(this, params);
    expr.getBody().accept(this, params);
    return null;
  }

  @Override
  public Void visitPi(PiExpression expr, Void params) {
    expr.getType().accept(this, params);
    expr.getCodomain().accept(this, params);
    return null;
  }

  @Override
  public Void visitSigma(SigmaExpression expr, Void params) {
    expr.getType().accept(this, params);
    return null;
  }

  @Override
  public Void visitUniverse(UniverseExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitError(ErrorExpression expr, Void params) {
    myErrorReporter.addError(expr, expr.getError().toString());
    return null;
  }

  @Override
  public Void visitTuple(TupleExpression expr, Void params) {
    SigmaExpression type = expr.getType();
    DependentLink link = type.getParameters();
    type.accept(this, params);
    for (Expression field : expr.getFields()) {
      field.accept(this, params);
      if (!field.getType().equals(link.getType())) {
        myErrorReporter.addError(field, "Expected type: " + link.getType() + ", found: " + field.getType());
      }
      if (!link.hasNext()) {
        myErrorReporter.addError(expr, "Too few abstractions in Sigma");
      }
      link = link.getNext();
    }
    return null;
  }

  @Override
  public Void visitProj(ProjExpression expr, Void params) {
    expr.getType().accept(this, params);
    expr.getExpression().accept(this, params);
    return null;
  }

  @Override
  public Void visitNew(NewExpression expr, Void params) {
    expr.getType().accept(this, params);
    return null;
  }

  @Override
  public Void visitLet(LetExpression letExpression, Void params) {
    letExpression.getType().accept(this, params);
    return null;
  }
}
