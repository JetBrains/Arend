package com.jetbrains.jetpad.vclang.typechecking.visitor;

import com.jetbrains.jetpad.vclang.naming.error.NamingError;
import com.jetbrains.jetpad.vclang.naming.reference.*;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.concrete.ConcreteExpressionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalError;

import java.util.List;
import java.util.Set;

public class ClassFieldChecker implements ConcreteExpressionVisitor<Void, Concrete.Expression> {
  private Referable myThisParameter;
  private final Set<? extends LocatedReferable> myFields;
  private final Set<TCReferable> myFutureFields;
  private final LocalErrorReporter myErrorReporter;

  ClassFieldChecker(Referable thisParameter, Set<? extends LocatedReferable> fields, Set<TCReferable> futureFields, LocalErrorReporter errorReporter) {
    myThisParameter = thisParameter;
    myFields = fields;
    myFutureFields = futureFields;
    myErrorReporter = errorReporter;
  }

  void setThisParameter(Referable thisParameter) {
    myThisParameter = thisParameter;
  }

  @Override
  public Concrete.Expression visitApp(Concrete.AppExpression expr, Void params) {
    expr.function = expr.function.accept(this, null);
    expr.argument.expression = expr.argument.expression.accept(this, null);
    return expr;
  }

  @Override
  public Concrete.Expression visitReference(Concrete.ReferenceExpression expr, Void params) {
    Referable ref = expr.getReferent();
    if (ref instanceof TCReferable && myFields.contains(ref)) {
      if (myFutureFields.size() < myFields.size() && myFutureFields.contains(ref)) {
        LocalError error = new NamingError("Fields may refer only to previous fields", expr.getData());
        myErrorReporter.report(error);
        return new Concrete.ErrorHoleExpression(expr.getData(), error);
      } else {
        return new Concrete.AppExpression(expr.getData(), expr, new Concrete.Argument(new Concrete.ReferenceExpression(expr.getData(), myThisParameter), false));
      }
    }
    return expr;
  }

  private void visitParameters(List<? extends Concrete.Parameter> parameters) {
    for (Concrete.Parameter parameter : parameters) {
      if (parameter instanceof Concrete.TypeParameter) {
        ((Concrete.TypeParameter) parameter).type = ((Concrete.TypeParameter) parameter).type.accept(this, null);
      }
    }
  }

  @Override
  public Concrete.Expression visitInferenceReference(Concrete.InferenceReferenceExpression expr, Void params) {
    return expr;
  }

  @Override
  public Concrete.Expression visitLam(Concrete.LamExpression expr, Void params) {
    visitParameters(expr.getParameters());
    expr.body = expr.body.accept(this, null);
    return expr;
  }

  @Override
  public Concrete.Expression visitPi(Concrete.PiExpression expr, Void params) {
    visitParameters(expr.getParameters());
    expr.codomain = expr.codomain.accept(this, null);
    return expr;
  }

  @Override
  public Concrete.Expression visitUniverse(Concrete.UniverseExpression expr, Void params) {
    return expr;
  }

  @Override
  public Concrete.Expression visitHole(Concrete.HoleExpression expr, Void params) {
    return expr;
  }

  @Override
  public Concrete.Expression visitGoal(Concrete.GoalExpression expr, Void params) {
    if (expr.expression != null) {
      expr.expression = expr.expression.accept(this, null);
    }
    return expr;
  }

  @Override
  public Concrete.Expression visitTuple(Concrete.TupleExpression expr, Void params) {
    for (int i = 0; i < expr.getFields().size(); i++) {
      expr.getFields().set(i, expr.getFields().get(i).accept(this, null));
    }
    return expr;
  }

  @Override
  public Concrete.Expression visitSigma(Concrete.SigmaExpression expr, Void params) {
    visitParameters(expr.getParameters());
    return expr;
  }

  @Override
  public Concrete.Expression visitBinOpSequence(Concrete.BinOpSequenceExpression expr, Void params) {
    throw new IllegalStateException();
  }

  @Override
  public Concrete.Expression visitCase(Concrete.CaseExpression expr, Void params) {
    for (int i = 0; i < expr.getExpressions().size(); i++) {
      expr.getExpressions().set(i, expr.getExpressions().get(i).accept(this, null));
    }
    for (Concrete.FunctionClause clause : expr.getClauses()) {
      clause.expression = clause.expression.accept(this, null);
    }
    return expr;
  }

  @Override
  public Concrete.Expression visitProj(Concrete.ProjExpression expr, Void params) {
    expr.expression = expr.expression.accept(this, null);
    return expr;
  }

  @Override
  public Concrete.Expression visitClassExt(Concrete.ClassExtExpression expr, Void params) {
    expr.baseClassExpression = expr.baseClassExpression.accept(this, null);
    for (Concrete.ClassFieldImpl classFieldImpl : expr.getStatements()) {
      classFieldImpl.implementation = classFieldImpl.implementation.accept(this, null);
    }
    return expr;
  }

  @Override
  public Concrete.Expression visitNew(Concrete.NewExpression expr, Void params) {
    expr.expression = expr.expression.accept(this, null);
    return expr;
  }

  @Override
  public Concrete.Expression visitLet(Concrete.LetExpression expr, Void params) {
    for (Concrete.LetClause clause : expr.getClauses()) {
      if (clause.resultType != null) {
        clause.resultType = clause.resultType.accept(this, null);
      }
      clause.term = clause.term.accept(this, null);
    }
    expr.expression = expr.expression.accept(this, null);
    return expr;
  }

  @Override
  public Concrete.Expression visitNumericLiteral(Concrete.NumericLiteral expr, Void params) {
    return expr;
  }
}
