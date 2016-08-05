package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.*;

public class FindIntervalVisitor extends BaseExpressionVisitor<Void, Boolean> {
  @Override
  public Boolean visitDefCall(DefCallExpression expr, Void params) {
    return expr.getDefinition() == Preprelude.INTERVAL || expr.getDefinition().containsInterval();
  }

  @Override
  public Boolean visitApp(AppExpression expr, Void params) {
    boolean found = expr.getFunction().accept(this, null);
    for (Expression arg : expr.getArguments()) {
      found |= arg.accept(this, null);
    }
    return found;
  }

  @Override
  public Boolean visitReference(ReferenceExpression expr, Void params) {
    return false;
  }

  @Override
  public Boolean visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    return false;
  }

  @Override
  public Boolean visitLam(LamExpression expr, Void params) {
    return expr.getBody().accept(this, null);
  }

  @Override
  public Boolean visitPi(PiExpression expr, Void params) {
    return expr.getCodomain().accept(this, null);
  }

  @Override
  public Boolean visitSigma(SigmaExpression expr, Void params) {
    boolean found = false;
    for (DependentLink param = expr.getParameters(); param.hasNext(); param = param.getNext()) {
      found |= param.getType().accept(this, null);
    }
    return found;
  }

  @Override
  public Boolean visitUniverse(UniverseExpression expr, Void params) {
    return false;
  }

  @Override
  public Boolean visitError(ErrorExpression expr, Void params) {
    return false;
  }

  @Override
  public Boolean visitTuple(TupleExpression expr, Void params) {
    boolean found = false;
    for (Expression field : expr.getFields()) {
      found |= field.accept(this, null);
    }
    return found;
  }

  @Override
  public Boolean visitProj(ProjExpression expr, Void params) {
    return expr.getExpression().accept(this, null);
  }

  @Override
  public Boolean visitNew(NewExpression expr, Void params) {
    return false;
  }

  @Override
  public Boolean visitLet(LetExpression expr, Void params) {
    return false;
  }

  @Override
  public Boolean visitOfType(OfTypeExpression expr, Void params) {
    return expr.getExpression().accept(this, null);
  }
}
