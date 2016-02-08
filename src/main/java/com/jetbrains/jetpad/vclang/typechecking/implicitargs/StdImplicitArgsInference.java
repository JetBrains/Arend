package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.InferenceBinding;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeMismatchError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Apps;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Reference;

public class StdImplicitArgsInference extends BaseImplicitArgsInference {
  public StdImplicitArgsInference(CheckTypeVisitor visitor) {
    super(visitor);
  }

  protected boolean fixImplicitArgs(CheckTypeVisitor.Result result, List<DependentLink> parameters, Abstract.Expression expr) {
    Substitution substitution = new Substitution();
    for (DependentLink parameter : parameters) {
      Expression binding = Reference(new InferenceBinding(parameter.getName(), parameter.getType()));
      result.expression = Apps(result.expression, binding, false, true);
      substitution.add(parameter, binding);
    }
    result.type = result.type.subst(substitution);
    return true;
  }

  protected CheckTypeVisitor.Result inferArg(CheckTypeVisitor.Result result, Abstract.Expression arg, boolean isExplicit, Abstract.Expression fun) {
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

  protected CheckTypeVisitor.Result inferArg(Abstract.Expression fun, Abstract.Expression arg, boolean isExplicit) {
    CheckTypeVisitor.Result result;
    if (fun instanceof Abstract.AppExpression) {
      Abstract.ArgumentExpression argument = ((Abstract.AppExpression) fun).getArgument();
      result = inferArg(((Abstract.AppExpression) fun).getFunction(), argument.getExpression(), argument.isExplicit());
    } else {
      result = myVisitor.typeCheck(fun, null);
    }
    return inferArg(result, arg, isExplicit, fun);
  }

  @Override
  public CheckTypeVisitor.Result infer(Abstract.AppExpression expr) {
    Abstract.ArgumentExpression arg = expr.getArgument();
    CheckTypeVisitor.Result result = inferArg(expr.getFunction(), arg.getExpression(), arg.isExplicit());
    updateResult(result);
    return result;
  }

  @Override
  public CheckTypeVisitor.Result infer(Abstract.BinOpExpression expr) {
    Concrete.Position position = expr instanceof Concrete.Expression ? ((Concrete.Expression) expr).getPosition() : ConcreteExpressionFactory.POSITION;
    CheckTypeVisitor.Result result = inferArg(inferArg(new Concrete.DefCallExpression(position, expr.getResolvedBinOpName()), expr.getLeft(), true), expr.getRight(), true, expr);
    updateResult(result);
    return result;
  }

  private void updateResult(CheckTypeVisitor.Result result) {
    if (result == null || result.equations.isEmpty()) {
      return;
    }

    List<InferenceBinding> bindings = new ArrayList<>();
    for (Expression expr = result.expression; expr instanceof AppExpression; expr = ((AppExpression) expr).getFunction()) {
      Expression argument = ((AppExpression) expr).getArgument().getExpression();
      if (argument instanceof ReferenceExpression && ((ReferenceExpression) argument).getBinding() instanceof InferenceBinding) {
        bindings.add((InferenceBinding) ((ReferenceExpression) argument).getBinding());
      }
    }
    if (bindings.isEmpty()) {
      return;
    }
    Collections.reverse(bindings);

    Substitution substitution = result.equations.getInferenceVariables(bindings);
    result.expression = result.expression.subst(substitution);
    result.type = result.type.subst(substitution);
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
