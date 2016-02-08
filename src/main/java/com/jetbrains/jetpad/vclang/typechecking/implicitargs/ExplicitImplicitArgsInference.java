package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeMismatchError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ExplicitImplicitArgsInference extends BaseImplicitArgsInference {
  protected ExplicitImplicitArgsInference(CheckTypeVisitor visitor) {
    super(visitor);
  }

  protected boolean fixImplicitArgs(CheckTypeVisitor.Result result, List<DependentLink> parameters, Abstract.Expression expr) {
    if (parameters.isEmpty()) {
      return true;
    } else {
      TypeCheckingError error = new TypeCheckingError("Cannot infer implicit arguments", expr);
      expr.setWellTyped(myVisitor.getContext(), new ErrorExpression(result.expression, error));
      myVisitor.getErrorReporter().report(error);
      return false;
    }
  }

  private CheckTypeVisitor.Result inferArg(CheckTypeVisitor.Result result, Abstract.Expression arg, boolean isExplicit, Abstract.Expression fun) {
    if (result == null) {
      return null;
    }

    if (isExplicit) {
      List<DependentLink> params = new ArrayList<>();
      result.type = result.type.getPiParameters(params, true, true);
      if (!fixImplicitArgs(result, params, fun)) {
        return null;
      }
    } else {
      result.type = result.type.normalize(NormalizeVisitor.Mode.WHNF);
    }

    if (!(result.type instanceof PiExpression)) {
      TypeCheckingError error = new TypeCheckingError("Expected an expression of a pi type", fun);
      fun.setWellTyped(myVisitor.getContext(), new ErrorExpression(result.expression, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }

    PiExpression actualType = (PiExpression) result.type;
    CheckTypeVisitor.Result argResult = myVisitor.typeCheck(arg, actualType.getParameters().getType());
    if (argResult == null) {
      return null;
    }

    if (actualType.getParameters().isExplicit() != isExplicit) {
      TypeCheckingError error = new TypeCheckingError("Expected an " + (actualType.getParameters().isExplicit() ? "explicit" : "implicit") + " argument", arg);
      arg.setWellTyped(myVisitor.getContext(), new ErrorExpression(argResult.expression, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }

    result.expression = new AppExpression(result.expression, new ArgumentExpression(argResult.expression, isExplicit, false));
    result.type = actualType.applyExpressions(Collections.singletonList(argResult.expression));
    result.equations.add(argResult.equations);
    return result;
  }

  private CheckTypeVisitor.Result inferArg(Abstract.Expression fun, Abstract.Expression arg, boolean isExplicit) {
    return inferArg(myVisitor.typeCheck(fun, null), arg, isExplicit, fun);
  }

  @Override
  public CheckTypeVisitor.Result infer(Abstract.AppExpression expr) {
    Abstract.ArgumentExpression arg = expr.getArgument();
    return inferArg(expr.getFunction(), arg.getExpression(), arg.isExplicit());
  }

  @Override
  public CheckTypeVisitor.Result infer(Abstract.BinOpExpression expr) {
    Concrete.Position position = expr instanceof Concrete.Expression ? ((Concrete.Expression) expr).getPosition() : null;
    return inferArg(inferArg(new Concrete.DefCallExpression(position, expr.getResolvedBinOpName()), expr.getLeft(), true), expr.getRight(), true, expr);
  }

  @Override
  public CheckTypeVisitor.Result inferTail(CheckTypeVisitor.Result result, Expression expectedType, Abstract.Expression expr) {
    List<DependentLink> actualParams = new ArrayList<>();
    Expression actualType = result.type.getPiParameters(actualParams, true, false);
    List<DependentLink> expectedParams = new ArrayList<>(actualParams.size());
    Expression expectedType1 = expectedType.getPiParameters(expectedParams, true, false);
    if (expectedParams.size() > actualParams.size()) {
      TypeCheckingError error = new TypeMismatchError(expectedType.normalize(NormalizeVisitor.Mode.HUMAN_NF), result.type.normalize(NormalizeVisitor.Mode.HUMAN_NF), expr);
      expr.setWellTyped(myVisitor.getContext(), new ErrorExpression(result.expression, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }
    if (expectedParams.size() == actualParams.size()) {
      return myVisitor.checkResult(expectedType, result, expr);
    }

    int argsNumber = actualParams.size() - expectedParams.size();
    for (int i = 0; i < expectedParams.size(); ++i) {
      if (expectedParams.get(i).isExplicit() != actualParams.get(argsNumber + i).isExplicit()) {
        TypeCheckingError error = new TypeMismatchError(expectedType.normalize(NormalizeVisitor.Mode.HUMAN_NF), result.type.normalize(NormalizeVisitor.Mode.HUMAN_NF), expr);
        expr.setWellTyped(myVisitor.getContext(), new ErrorExpression(result.expression, error));
        myVisitor.getErrorReporter().report(error);
        return null;
      }
    }

    for (int i = 0; i < argsNumber; ++i) {
      if (actualParams.get(i).isExplicit()) {
        TypeCheckingError error = new TypeMismatchError(expectedType.normalize(NormalizeVisitor.Mode.HUMAN_NF), result.type.normalize(NormalizeVisitor.Mode.HUMAN_NF), expr);
        expr.setWellTyped(myVisitor.getContext(), new ErrorExpression(result.expression, error));
        myVisitor.getErrorReporter().report(error);
        return null;
      }
    }

    result.type = actualType.fromPiParameters(actualParams.subList(argsNumber, actualParams.size()));
    if (!fixImplicitArgs(result, actualParams.subList(0, argsNumber), expr)) {
      return null;
    }
    return myVisitor.checkResult(expectedType1.fromPiParameters(expectedParams), result, expr);
  }
}
