package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.*;

public class ValidateTypeVisitor extends BaseExpressionVisitor<Void, String> {
  @Override
  public String visitDefCall(DefCallExpression expr, Void params) {
    return expr.getType().accept(this, params);
  }

  @Override
  public String visitApp(AppExpression expr, Void params) {
    Expression fun = expr.getFunction();
    Expression funType = fun.getType();
    if (!(funType instanceof PiExpression)) {
      return "App LHS is not Pi";
    }
    return null;
  }

  @Override
  public String visitReference(ReferenceExpression expr, Void params) {
    return expr.getBinding().getType().accept(this, params);
  }

  @Override
  public String visitLam(LamExpression expr, Void params) {
    String res = expr.getType().accept(this, params);
    if (res != null)
      return res;
    res = expr.getBody().accept(this, params);
    return res;
  }

  @Override
  public String visitPi(PiExpression expr, Void params) {
    String res = expr.getType().accept(this, params);
    if (res != null)
      return res;
    res = expr.getCodomain().accept(this, params);
    return res;
  }

  @Override
  public String visitSigma(SigmaExpression expr, Void params) {
    String res = expr.getType().accept(this, params);
    return res;
  }

  @Override
  public String visitUniverse(UniverseExpression expr, Void params) {
    return null;
  }

  @Override
  public String visitError(ErrorExpression expr, Void params) {
    return expr.toString();
  }

  @Override
  public String visitTuple(TupleExpression expr, Void params) {
    String res = expr.getType().accept(this, params);
    if (res != null)
      return res;
    for (Expression field : expr.getFields()) {
      res = field.accept(this, params);
      if (res != null)
        return res;
    }
    return null;
  }

  @Override
  public String visitProj(ProjExpression expr, Void params) {
    String res = expr.getType().accept(this, params);
    if (res != null)
      return res;
    return expr.getExpression().accept(this, params);
  }

  @Override
  public String visitNew(NewExpression expr, Void params) {
    return expr.getType().accept(this, params);
  }

  @Override
  public String visitLet(LetExpression letExpression, Void params) {
    String res = letExpression.getType().accept(this, params);
    if (res != null)
      return res;
    return null;
  }
}
