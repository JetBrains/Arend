package org.arend.typechecking.visitor;

import org.arend.core.expr.*;
import org.arend.ext.core.expr.CoreExpression;

import java.util.function.Predicate;

public class FindSubexpressionVisitor extends SearchVisitor<Void> {
  private final Predicate<CoreExpression> myPredicate;

  public FindSubexpressionVisitor(Predicate<CoreExpression> predicate) {
    myPredicate = predicate;
  }

  @Override
  protected boolean processDefCall(DefCallExpression expression, Void param) {
    return myPredicate.test(expression) || super.processDefCall(expression, param);
  }

  @Override
  public Boolean visitApp(AppExpression expression, Void param) {
    return myPredicate.test(expression) || super.visitApp(expression, param);
  }

  @Override
  public Boolean visitLet(LetExpression expression, Void param) {
    return myPredicate.test(expression) || super.visitLet(expression, param);
  }

  @Override
  public Boolean visitCase(CaseExpression expr, Void param) {
    return myPredicate.test(expr) || super.visitCase(expr, param);
  }

  @Override
  public Boolean visitLam(LamExpression expression, Void param) {
    return myPredicate.test(expression) || super.visitLam(expression, param);
  }

  @Override
  public Boolean visitPi(PiExpression expression, Void param) {
    return myPredicate.test(expression) || super.visitPi(expression, param);
  }

  @Override
  public Boolean visitSigma(SigmaExpression expression, Void param) {
    return myPredicate.test(expression) || super.visitSigma(expression, param);
  }

  @Override
  public Boolean visitTuple(TupleExpression expression, Void param) {
    return myPredicate.test(expression) || super.visitTuple(expression, param);
  }

  @Override
  public Boolean visitProj(ProjExpression expression, Void param) {
    return myPredicate.test(expression) || super.visitProj(expression, param);
  }

  @Override
  public Boolean visitNew(NewExpression expression, Void param) {
    return myPredicate.test(expression) || super.visitNew(expression, param);
  }

  @Override
  public Boolean visitPEval(PEvalExpression expr, Void param) {
    return myPredicate.test(expr) || super.visitPEval(expr, param);
  }

  @Override
  public Boolean visitUniverse(UniverseExpression expression, Void param) {
    return myPredicate.test(expression) || super.visitUniverse(expression, param);
  }

  @Override
  public Boolean visitError(ErrorExpression expression, Void param) {
    return myPredicate.test(expression) || super.visitError(expression, param);
  }

  @Override
  public Boolean visitReference(ReferenceExpression expression, Void param) {
    return myPredicate.test(expression) || super.visitReference(expression, param);
  }

  @Override
  public Boolean visitInferenceReference(InferenceReferenceExpression expression, Void param) {
    return myPredicate.test(expression) || super.visitInferenceReference(expression, param);
  }

  @Override
  public Boolean visitSubst(SubstExpression expr, Void param) {
    return myPredicate.test(expr) || super.visitSubst(expr, param);
  }

  @Override
  public Boolean visitOfType(OfTypeExpression expression, Void param) {
    return myPredicate.test(expression) || super.visitOfType(expression, param);
  }

  @Override
  public Boolean visitInteger(IntegerExpression expr, Void params) {
    return myPredicate.test(expr) || super.visitInteger(expr, params);
  }
}
