package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.expr.*;

public class LevelSubstVisitor extends BaseExpressionVisitor<Void, Expression> {
  private LevelSubstitution mySubst;

  public LevelSubstVisitor(LevelSubstitution subst) {
    mySubst = subst;
  }

  @Override
  public Expression visitDefCall(DefCallExpression expr, Void params) {
    return null;
  }

  @Override
  public Expression visitApp(AppExpression expr, Void params) {
    return null;
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Void params) {
    return null;
  }

  @Override
  public Expression visitLam(LamExpression expr, Void params) {

  }

  @Override
  public Expression visitPi(PiExpression expr, Void params) {
    return expr.accept(this, params);
  }

  @Override
  public Expression visitSigma(SigmaExpression expr, Void params) {
    return expr.accept(this, params);
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr, Void params) {
    return null;
  }

  @Override
  public Expression visitError(ErrorExpression expr, Void params) {
    return expr.accept(this, params);
  }

  @Override
  public Expression visitTuple(TupleExpression expr, Void params) {
    return expr.accept(this, params);
  }

  @Override
  public Expression visitProj(ProjExpression expr, Void params) {
    return expr.accept(this, params);
  }

  @Override
  public Expression visitNew(NewExpression expr, Void params) {
    return expr.accept(this, params);
  }

  @Override
  public Expression visitLet(LetExpression expr, Void params) {
    return expr.accept(this, params);
  }

  @Override
  public Expression visitOfType(OfTypeExpression expr, Void params) {
    return expr.accept(this, params);
  }

}
