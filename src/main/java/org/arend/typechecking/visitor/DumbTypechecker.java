package org.arend.typechecking.visitor;

import org.arend.core.definition.*;
import org.arend.core.expr.*;
import org.arend.core.expr.type.ExpectedType;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.type.TypeExpression;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.core.sort.Sort;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.error.LocalErrorReporter;
import org.arend.typechecking.error.local.GoalError;
import org.arend.typechecking.error.local.TypeMismatchError;
import org.arend.typechecking.error.local.TypecheckingError;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.arend.typechecking.result.TypecheckingResult;

import java.util.*;

public class DumbTypechecker extends BaseTypechecker {
  private Concrete.Definition myConcreteDefinition;
  private Definition myDefinition;
  private boolean myRecursive;

  public DumbTypechecker(TypecheckerState state, LocalErrorReporter errorReporter) {
    super(state, new HashMap<>());
    this.errorReporter = errorReporter;
    // TODO: Set implicitArgsInference
  }

  public void setCurrentDefinition(Concrete.Definition concreteDefinition, Definition definition) {
    myConcreteDefinition = concreteDefinition;
    myDefinition = definition;
    myRecursive = false;
  }

  public boolean isRecursive() {
    return myRecursive;
  }

  @Override
  protected TypecheckingResult checkExpr(Concrete.Expression expr, ExpectedType expectedType) {
    return expr == null ? null : expr.accept(this, expectedType);
  }

  private TypecheckingResult checkResultExpr(Expression expectedType, TypecheckingResult result, Concrete.Expression expr) {
    if (new CompareVisitor(DummyEquations.getInstance(), Equations.CMP.LE, expr).normalizedCompare(result.type, expectedType)) { // TODO
      return result;
    }

    if (!result.type.isError()) {
      errorReporter.report(new TypeMismatchError(expectedType, result.type, expr));
    }
    return null;
  }

  @Override
  public Type checkType(Concrete.Expression expr, ExpectedType expectedType, boolean isFinal) {
    if (expr == null) {
      return null;
    }

    if (expectedType instanceof Expression) {
      if (((Expression) expectedType).getStuckInferenceVariable() != null) {
        expectedType = ExpectedType.OMEGA;
      }
    }

    TypecheckingResult result = expr.accept(this, expectedType);
    if (result == null) {
      return null;
    }
    if (result.expression instanceof Type) {
      return (Type) result.expression;
    }

    UniverseExpression universe = result.type.checkedCast(UniverseExpression.class);
    return new TypeExpression(result.expression, universe == null ? Sort.UNKNOWN : universe.getSort());
  }

  @Override
  public boolean isDumb() {
    return true;
  }

  @Override
  public TypecheckingResult typecheckClassExt(List<? extends Concrete.ClassFieldImpl> classFieldImpls, ExpectedType expectedType, Expression implExpr, ClassCallExpression classCallExpr, Set<ClassField> pseudoImplemented, Concrete.Expression expr) {
    // TODO
    return null;
  }

  @Override
  public Equations getEquations() {
    return DummyEquations.getInstance();
  }

  @Override
  protected TypecheckingResult checkResult(ExpectedType expectedType, TypecheckingResult result, Concrete.Expression expr) {
    // TODO
    return result;
  }

  @Override
  public TypecheckingResult visitReference(Concrete.ReferenceExpression expr, ExpectedType expectedType) {
    if (expr.getReferent() == myConcreteDefinition.getData()) {
      myConcreteDefinition.setRecursive(true);
      if (myDefinition instanceof FunctionDefinition && ((FunctionDefinition) myDefinition).getResultType() == null) {
        return null;
      }
    }

    return super.visitReference(expr, expectedType);
  }

  @Override
  public TypecheckingResult visitGoal(Concrete.GoalExpression expr, ExpectedType expectedType) {
    TypecheckingResult exprResult = expr.getExpression() == null ? null : checkExpr(expr.getExpression(), expectedType);
    TypecheckingError error = new GoalError(expr.getName(), context, expectedType, exprResult == null ? null : exprResult.type, Collections.emptyList(), expr);
    errorReporter.report(error);
    Expression result = new ErrorExpression(exprResult == null ? null : exprResult.expression, error);
    return new TypecheckingResult(result, expectedType instanceof Expression ? (Expression) expectedType : result);
  }

  @Override
  public TypecheckingResult visitLam(Concrete.LamExpression expr, ExpectedType expectedType) {
    // TODO
    return null;
  }

  @Override
  public TypecheckingResult visitPi(Concrete.PiExpression expr, ExpectedType expectedType) {
    // TODO
    return null;
  }

  @Override
  public TypecheckingResult visitBinOpSequence(Concrete.BinOpSequenceExpression expr, ExpectedType expectedType) {
    // TODO
    return null;
  }

  @Override
  public TypecheckingResult visitCase(Concrete.CaseExpression expr, ExpectedType expectedType) {
    // TODO
    return null;
  }

  @Override
  public TypecheckingResult visitClassExt(Concrete.ClassExtExpression expr, ExpectedType expectedType) {
    // TODO
    return null;
  }

  @Override
  public TypecheckingResult visitNew(Concrete.NewExpression expr, ExpectedType expectedType) {
    // TODO
    return null;
  }

  @Override
  public TypecheckingResult visitLet(Concrete.LetExpression expr, ExpectedType expectedType) {
    // TODO
    return null;
  }
}
