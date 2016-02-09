package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.IgnoreBinding;
import com.jetbrains.jetpad.vclang.term.definition.InferenceBinding;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeMismatchError;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

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

  protected CheckTypeVisitor.Result inferArg(Abstract.Expression fun, Abstract.Expression arg, boolean isExplicit, Expression expectedType) {
    CheckTypeVisitor.Result result;
    if (fun instanceof Abstract.AppExpression) {
      Abstract.ArgumentExpression argument = ((Abstract.AppExpression) fun).getArgument();
      result = inferArg(((Abstract.AppExpression) fun).getFunction(), argument.getExpression(), argument.isExplicit(), null);
    } else {
      result = myVisitor.typeCheck(fun, null);
    }
    if (result == null) return null;

    if (result.expression instanceof ConCallExpression && ((ConCallExpression) result.expression).getDefinition() == Prelude.PATH_CON) {
      Expression interval = DataCall(Prelude.INTERVAL);
      CheckTypeVisitor.Result argResult = myVisitor.typeCheck(arg, Pi(interval, Reference(new IgnoreBinding(null, Universe()))));
      if (argResult == null) return null;
      Expression type = argResult.type.normalize(NormalizeVisitor.Mode.WHNF);
      if (type instanceof PiExpression) {
        PiExpression piType = (PiExpression) type;
        DependentLink params = piType.getParameters();
        Expression domType = params.getType().normalize(NormalizeVisitor.Mode.WHNF);
        if (CompareVisitor.compare(argResult.equations, Equations.CMP.EQ, interval, domType)) {
          Expression lamExpr;
          if (params.getNext().hasNext()) {
            DependentLink lamParam = param("i", interval);
            lamExpr = Lam(lamParam, Pi(params.getNext(), piType.getCodomain()).subst(params, Reference(lamParam)));
          } else {
            lamExpr = Lam(params, piType.getCodomain());
          }
          Expression expr1 = Apps(argResult.expression, ConCall(Prelude.LEFT));
          Expression expr2 = Apps(argResult.expression, ConCall(Prelude.RIGHT));
          return new CheckTypeVisitor.Result(Apps(ConCall(Prelude.PATH_CON, lamExpr, expr1, expr2), argResult.expression), Apps(DataCall(Prelude.PATH), lamExpr, expr1, expr2), argResult.equations);
        }
      }

      TypeCheckingError error = new TypeCheckingError("Expected an expression of a type of the form I -> _", arg);
      arg.setWellTyped(myVisitor.getContext(), new ErrorExpression(result.expression, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }

    return inferArg(result, arg, isExplicit, fun);
  }

  @Override
  public CheckTypeVisitor.Result infer(Abstract.AppExpression expr, Expression expectedType) {
    Abstract.ArgumentExpression arg = expr.getArgument();
    CheckTypeVisitor.Result result = inferArg(expr.getFunction(), arg.getExpression(), arg.isExplicit(), expectedType);
    myVisitor.updateAppResult(result, expr);
    return result;
  }

  @Override
  public CheckTypeVisitor.Result infer(Abstract.BinOpExpression expr, Expression expectedType) {
    Concrete.Position position = expr instanceof Concrete.Expression ? ((Concrete.Expression) expr).getPosition() : ConcreteExpressionFactory.POSITION;
    CheckTypeVisitor.Result result = inferArg(inferArg(new Concrete.DefCallExpression(position, expr.getResolvedBinOpName()), expr.getLeft(), true, null), expr.getRight(), true, expr);
    myVisitor.updateAppResult(result, expr);
    return result;
  }

  @Override
  public CheckTypeVisitor.Result inferTail(CheckTypeVisitor.Result result, Expression expectedType, Abstract.Expression expr) {
    List<DependentLink> actualParams = new ArrayList<>();
    Expression actualType = result.type.getPiParameters(actualParams, true, true);
    List<DependentLink> expectedParams = new ArrayList<>(actualParams.size());
    Expression expectedType1 = expectedType.getPiParameters(expectedParams, true, true);
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
    result.type = actualType.fromPiParameters(actualParams.subList(argsNumber, actualParams.size()));
    if (!fixImplicitArgs(result, actualParams.subList(0, argsNumber), expr)) {
      return null;
    }
    return myVisitor.checkResult(expectedType1.fromPiParameters(expectedParams), result, expr);
  }
}
