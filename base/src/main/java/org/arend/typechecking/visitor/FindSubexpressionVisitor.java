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
  protected boolean checkPathArgumentType() {
    return false;
  }

  @Override
  protected CoreExpression.FindAction processDefCall(DefCallExpression expression, Void param) {
    return myFunction.apply(expression);
  }

  @Override
  public Boolean visitPath(PathExpression expr, Void param) {
    return switch (myFunction.apply(expr)) {
      case STOP -> true;
      case SKIP -> false;
      default -> super.visitPath(expr, param);
    };
  }

  @Override
  public Boolean visitAt(AtExpression expr, Void params) {
    return switch (myFunction.apply(expr)) {
      case STOP -> true;
      case SKIP -> false;
      default -> super.visitAt(expr, params);
    };
  }

  @Override
  public Boolean visitApp(AppExpression expression, Void param) {
    return switch (myFunction.apply(expression)) {
      case STOP -> true;
      case SKIP -> false;
      default -> super.visitApp(expression, param);
    };
  }

  @Override
  public Boolean visitLet(LetExpression expression, Void param) {
    return switch (myFunction.apply(expression)) {
      case STOP -> true;
      case SKIP -> false;
      default -> super.visitLet(expression, param);
    };
  }

  @Override
  public Boolean visitCase(CaseExpression expression, Void param) {
    return switch (myFunction.apply(expression)) {
      case STOP -> true;
      case SKIP -> false;
      default -> super.visitCase(expression, param);
    };
  }

  @Override
  public Boolean visitLam(LamExpression expression, Void param) {
    return switch (myFunction.apply(expression)) {
      case STOP -> true;
      case SKIP -> false;
      default -> super.visitLam(expression, param);
    };
  }

  @Override
  public Boolean visitPi(PiExpression expression, Void param) {
    return switch (myFunction.apply(expression)) {
      case STOP -> true;
      case SKIP -> false;
      default -> super.visitPi(expression, param);
    };
  }

  @Override
  public Boolean visitSigma(SigmaExpression expression, Void param) {
    return switch (myFunction.apply(expression)) {
      case STOP -> true;
      case SKIP -> false;
      default -> super.visitSigma(expression, param);
    };
  }

  @Override
  public Boolean visitTuple(TupleExpression expression, Void param) {
    return switch (myFunction.apply(expression)) {
      case STOP -> true;
      case SKIP -> false;
      default -> super.visitTuple(expression, param);
    };
  }

  @Override
  public Boolean visitProj(ProjExpression expression, Void param) {
    return switch (myFunction.apply(expression)) {
      case STOP -> true;
      case SKIP -> false;
      default -> super.visitProj(expression, param);
    };
  }

  @Override
  public Boolean visitNew(NewExpression expression, Void param) {
    return switch (myFunction.apply(expression)) {
      case STOP -> true;
      case SKIP -> false;
      default -> super.visitNew(expression, param);
    };
  }

  @Override
  public Boolean visitPEval(PEvalExpression expression, Void param) {
    return switch (myFunction.apply(expression)) {
      case STOP -> true;
      case SKIP -> false;
      default -> super.visitPEval(expression, param);
    };
  }

  @Override
  public Boolean visitBox(BoxExpression expr, Void params) {
    return switch (myFunction.apply(expr)) {
      case STOP -> true;
      case SKIP -> false;
      default -> super.visitBox(expr, null);
    };
  }

  @Override
  public Boolean visitUniverse(UniverseExpression expression, Void param) {
    return switch (myFunction.apply(expression)) {
      case STOP -> true;
      case SKIP -> false;
      default -> super.visitUniverse(expression, param);
    };
  }

  @Override
  public Boolean visitError(ErrorExpression expression, Void param) {
    return switch (myFunction.apply(expression)) {
      case STOP -> true;
      case SKIP -> false;
      default -> super.visitError(expression, param);
    };
  }

  @Override
  public Boolean visitReference(ReferenceExpression expression, Void param) {
    return switch (myFunction.apply(expression)) {
      case STOP -> true;
      case SKIP -> false;
      default -> super.visitReference(expression, param);
    };
  }

  @Override
  public Boolean visitInferenceReference(InferenceReferenceExpression expression, Void param) {
    return switch (myFunction.apply(expression)) {
      case STOP -> true;
      case SKIP -> false;
      default -> super.visitInferenceReference(expression, param);
    };
  }

  @Override
  public Boolean visitSubst(SubstExpression expression, Void param) {
    return switch (myFunction.apply(expression)) {
      case STOP -> true;
      case SKIP -> false;
      default -> super.visitSubst(expression, param);
    };
  }

  @Override
  public Boolean visitOfType(OfTypeExpression expression, Void param) {
    return switch (myFunction.apply(expression)) {
      case STOP -> true;
      case SKIP -> false;
      default -> super.visitOfType(expression, param);
    };
  }

  @Override
  public Boolean visitInteger(IntegerExpression expression, Void param) {
    return switch (myFunction.apply(expression)) {
      case STOP -> true;
      case SKIP -> false;
      default -> super.visitInteger(expression, param);
    };
  }

  @Override
  public Boolean visitString(StringExpression expr, Void param) {
    return switch (myFunction.apply(expr)) {
      case STOP -> true;
      case SKIP -> false;
      default -> super.visitString(expr, param);
    };
  }

  @Override
  public Boolean visitQName(QNameExpression expr, Void params) {
    return switch (myFunction.apply(expr)) {
      case STOP -> true;
      case SKIP -> false;
      default -> super.visitQName(expr, params);
    };
  }

  @Override
  public Boolean visitTypeConstructor(TypeConstructorExpression expr, Void param) {
    return switch (myFunction.apply(expr)) {
      case STOP -> true;
      case SKIP -> false;
      default -> super.visitTypeConstructor(expr, param);
    };
  }

  @Override
  public Boolean visitTypeDestructor(TypeDestructorExpression expr, Void param) {
    return switch (myFunction.apply(expr)) {
      case STOP -> true;
      case SKIP -> false;
      default -> super.visitTypeDestructor(expr, param);
    };
  }

  @Override
  public Boolean visitArray(ArrayExpression expr, Void params) {
    return switch (myFunction.apply(expr)) {
      case STOP -> true;
      case SKIP -> false;
      default -> super.visitArray(expr, params);
    };
  }
}
