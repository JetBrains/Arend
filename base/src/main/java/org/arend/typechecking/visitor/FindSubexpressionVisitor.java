package org.arend.typechecking.visitor;

import org.arend.core.expr.*;
import org.arend.ext.core.expr.CoreExpression;

import java.util.function.Function;

public class FindSubexpressionVisitor extends SearchVisitor<Void> {
  private final Function<CoreExpression, CoreExpression.FindAction> myFunction;

  public FindSubexpressionVisitor(Function<CoreExpression, CoreExpression.FindAction> function) {
    myFunction = function;
  }

  @Override
  protected boolean preserveOrder() {
    return true;
  }

  @Override
  public Boolean visitDefCall(DefCallExpression expression, Void param) {
    switch (myFunction.apply(expression)) {
      case STOP: return true;
      case SKIP: return false;
      default: return super.visitDefCall(expression, param);
    }
  }

  @Override
  public Boolean visitConCall(ConCallExpression expression, Void param) {
    switch (myFunction.apply(expression)) {
      case STOP: return true;
      case SKIP: return false;
      default: return super.visitConCall(expression, param);
    }
  }

  @Override
  public Boolean visitApp(AppExpression expression, Void param) {
    switch (myFunction.apply(expression)) {
      case STOP: return true;
      case SKIP: return false;
      default: return super.visitApp(expression, param);
    }
  }

  @Override
  public Boolean visitLet(LetExpression expression, Void param) {
    switch (myFunction.apply(expression)) {
      case STOP: return true;
      case SKIP: return false;
      default: return super.visitLet(expression, param);
    }
  }

  @Override
  public Boolean visitCase(CaseExpression expression, Void param) {
    switch (myFunction.apply(expression)) {
      case STOP: return true;
      case SKIP: return false;
      default: return super.visitCase(expression, param);
    }
  }

  @Override
  public Boolean visitLam(LamExpression expression, Void param) {
    switch (myFunction.apply(expression)) {
      case STOP: return true;
      case SKIP: return false;
      default: return super.visitLam(expression, param);
    }
  }

  @Override
  public Boolean visitPi(PiExpression expression, Void param) {
    switch (myFunction.apply(expression)) {
      case STOP: return true;
      case SKIP: return false;
      default: return super.visitPi(expression, param);
    }
  }

  @Override
  public Boolean visitSigma(SigmaExpression expression, Void param) {
    switch (myFunction.apply(expression)) {
      case STOP: return true;
      case SKIP: return false;
      default: return super.visitSigma(expression, param);
    }
  }

  @Override
  public Boolean visitTuple(TupleExpression expression, Void param) {
    switch (myFunction.apply(expression)) {
      case STOP: return true;
      case SKIP: return false;
      default: return super.visitTuple(expression, param);
    }
  }

  @Override
  public Boolean visitProj(ProjExpression expression, Void param) {
    switch (myFunction.apply(expression)) {
      case STOP: return true;
      case SKIP: return false;
      default: return super.visitProj(expression, param);
    }
  }

  @Override
  public Boolean visitNew(NewExpression expression, Void param) {
    switch (myFunction.apply(expression)) {
      case STOP: return true;
      case SKIP: return false;
      default: return super.visitNew(expression, param);
    }
  }

  @Override
  public Boolean visitPEval(PEvalExpression expression, Void param) {
    switch (myFunction.apply(expression)) {
      case STOP: return true;
      case SKIP: return false;
      default: return super.visitPEval(expression, param);
    }
  }

  @Override
  public Boolean visitUniverse(UniverseExpression expression, Void param) {
    switch (myFunction.apply(expression)) {
      case STOP: return true;
      case SKIP: return false;
      default: return super.visitUniverse(expression, param);
    }
  }

  @Override
  public Boolean visitError(ErrorExpression expression, Void param) {
    switch (myFunction.apply(expression)) {
      case STOP: return true;
      case SKIP: return false;
      default: return super.visitError(expression, param);
    }
  }

  @Override
  public Boolean visitReference(ReferenceExpression expression, Void param) {
    switch (myFunction.apply(expression)) {
      case STOP: return true;
      case SKIP: return false;
      default: return super.visitReference(expression, param);
    }
  }

  @Override
  public Boolean visitInferenceReference(InferenceReferenceExpression expression, Void param) {
    switch (myFunction.apply(expression)) {
      case STOP: return true;
      case SKIP: return false;
      default: return super.visitInferenceReference(expression, param);
    }
  }

  @Override
  public Boolean visitSubst(SubstExpression expression, Void param) {
    switch (myFunction.apply(expression)) {
      case STOP: return true;
      case SKIP: return false;
      default: return super.visitSubst(expression, param);
    }
  }

  @Override
  public Boolean visitOfType(OfTypeExpression expression, Void param) {
    switch (myFunction.apply(expression)) {
      case STOP: return true;
      case SKIP: return false;
      default: return super.visitOfType(expression, param);
    }
  }

  @Override
  public Boolean visitInteger(IntegerExpression expression, Void param) {
    switch (myFunction.apply(expression)) {
      case STOP: return true;
      case SKIP: return false;
      default: return super.visitInteger(expression, param);
    }
  }

  @Override
  public Boolean visitTypeCoerce(TypeCoerceExpression expr, Void param) {
    switch (myFunction.apply(expr)) {
      case STOP: return true;
      case SKIP: return false;
      default: return super.visitTypeCoerce(expr, param);
    }
  }

  @Override
  public Boolean visitArray(ArrayExpression expr, Void params) {
    switch (myFunction.apply(expr)) {
      case STOP: return true;
      case SKIP: return false;
      default: return super.visitArray(expr, params);
    }
  }
}
