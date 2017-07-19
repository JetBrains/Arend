package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.core.context.binding.inference.FunctionInferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.TypeClassInferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.TypedSingleDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.type.ExpectedType;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.prettyprint.StringPrettyPrintable;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.TypeMismatchError;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;

public class StdImplicitArgsInference extends BaseImplicitArgsInference {
  public StdImplicitArgsInference(CheckTypeVisitor visitor) {
    super(visitor);
  }

  protected CheckTypeVisitor.TResult fixImplicitArgs(CheckTypeVisitor.TResult result, List<? extends DependentLink> implicitParameters, Abstract.Expression expr) {
    ExprSubstitution substitution = new ExprSubstitution();
    int i = 0;
    for (DependentLink parameter : implicitParameters) {
      Expression type = parameter.getTypeExpr().subst(substitution, LevelSubstitution.EMPTY).normalize(NormalizeVisitor.Mode.WHNF);
      InferenceVariable infVar = null;
      if (result instanceof CheckTypeVisitor.DefCallResult) {
        CheckTypeVisitor.DefCallResult defCallResult = (CheckTypeVisitor.DefCallResult) result;
        ClassField classifyingField = defCallResult.getDefinition().getClassifyingFieldOfParameter(defCallResult.getArguments().size());
        if (classifyingField != null) {
          infVar = new TypeClassInferenceVariable(parameter.getName(), type, defCallResult.getDefCall(), i, null, classifyingField);
        }
      }
      if (infVar == null) {
        infVar = new FunctionInferenceVariable(parameter.getName(), type, i + 1, result instanceof CheckTypeVisitor.DefCallResult ? ((CheckTypeVisitor.DefCallResult) result).getDefinition() : null, expr);
      }
      Expression binding = new InferenceReferenceExpression(infVar, myVisitor.getEquations());
      result = result.applyExpression(binding);
      substitution.add(parameter, binding);
      i++;
    }
    return result;
  }

  protected CheckTypeVisitor.TResult inferArg(CheckTypeVisitor.TResult result, Abstract.Expression arg, boolean isExplicit, Abstract.Expression fun) {
    if (result == null || result instanceof CheckTypeVisitor.Result && ((CheckTypeVisitor.Result) result).expression.isInstance(ErrorExpression.class)) {
      return result;
    }

    if (isExplicit) {
      if (result instanceof CheckTypeVisitor.DefCallResult) {
        CheckTypeVisitor.DefCallResult defCallResult = (CheckTypeVisitor.DefCallResult) result;
        if (defCallResult.getDefinition() == Prelude.PATH_CON && defCallResult.getArguments().isEmpty()) {
          SingleDependentLink lamParam = new TypedSingleDependentLink(true, "i", Interval());
          UniverseExpression type = new UniverseExpression(new Sort(defCallResult.getSortArgument().getPLevel(), defCallResult.getSortArgument().getHLevel().add(1)));
          Expression binding = new InferenceReferenceExpression(new FunctionInferenceVariable("A", type, 1, Prelude.PATH_CON, fun), myVisitor.getEquations());
          Sort sort = type.getSort().succ();
          result = result.applyExpression(new LamExpression(sort, lamParam, binding));

          CheckTypeVisitor.Result argResult = myVisitor.checkExpr(arg, new PiExpression(sort, lamParam, binding));
          if (argResult == null) {
            return null;
          }

          Expression expr1 = new AppExpression(argResult.expression, Left());
          Expression expr2 = new AppExpression(argResult.expression, Right());
          return ((CheckTypeVisitor.DefCallResult) result).applyExpressions(Arrays.asList(expr1, expr2, argResult.expression));
        }
      }

      result = fixImplicitArgs(result, result.getImplicitParameters(), fun);
    }

    DependentLink param = result.getParameter();
    if (!param.hasNext()) {
      CheckTypeVisitor.Result result1 = result.toResult(myVisitor.getEquations());
      LocalTypeCheckingError error = new TypeMismatchError(new StringPrettyPrintable("A pi type"), result1.type, fun);
      fun.setWellTyped(myVisitor.getContext(), new ErrorExpression(result1.expression, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }

    CheckTypeVisitor.Result argResult = myVisitor.checkExpr(arg, param.getTypeExpr());
    if (argResult == null) {
      return null;
    }

    if (param.isExplicit() != isExplicit) {
      LocalTypeCheckingError error = new LocalTypeCheckingError("Expected an " + (param.isExplicit() ? "explicit" : "implicit") + " argument", arg);
      arg.setWellTyped(myVisitor.getContext(), new ErrorExpression(argResult.expression, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }

    return result.applyExpression(argResult.expression);
  }

  protected CheckTypeVisitor.TResult inferArg(Abstract.Expression fun, Abstract.Expression arg, boolean isExplicit, ExpectedType expectedType) {
    CheckTypeVisitor.TResult result;
    if (fun instanceof Abstract.AppExpression) {
      Abstract.Argument argument = ((Abstract.AppExpression) fun).getArgument();
      result = checkBinOpInferArg(((Abstract.AppExpression) fun).getFunction(), argument.getExpression(), argument.isExplicit(), expectedType);
    } else {
      if (fun instanceof Abstract.ReferenceExpression) {
        Abstract.ReferenceExpression defCall = (Abstract.ReferenceExpression) fun;
        result = defCall.getExpression() == null && !(defCall.getReferent() instanceof Abstract.Definition) ? myVisitor.getLocalVar(defCall) : myVisitor.getTypeCheckingDefCall().typeCheckDefCall(defCall);
      } else {
        result = myVisitor.checkExpr(fun, null);
      }

      if (result instanceof CheckTypeVisitor.DefCallResult && isExplicit && expectedType != null) {
        CheckTypeVisitor.DefCallResult defCallResult = (CheckTypeVisitor.DefCallResult) result;
        if (defCallResult.getDefinition() instanceof Constructor && defCallResult.getArguments().size() < DependentLink.Helper.size(((Constructor) defCallResult.getDefinition()).getDataTypeParameters())) {
          DataCallExpression dataCall = expectedType instanceof Expression ? ((Expression) expectedType).normalize(NormalizeVisitor.Mode.WHNF).checkedCast(DataCallExpression.class) : null;
          if (dataCall != null) {
            if (((Constructor) defCallResult.getDefinition()).getDataType() != dataCall.getDefinition()) {
              LocalTypeCheckingError error = new TypeMismatchError(dataCall, ((Constructor) defCallResult.getDefinition()).getDataType(), fun);
              arg.setWellTyped(myVisitor.getContext(), new ErrorExpression(null, error));
              myVisitor.getErrorReporter().report(error);
              return null;
            }

            List<? extends Expression> args = dataCall.getDefCallArguments();
            List<Expression> args1 = new ArrayList<>(args.size());
            args1.addAll(defCallResult.getArguments());
            args1.addAll(args.subList(defCallResult.getArguments().size(), args.size()));
            args1 = ((Constructor) defCallResult.getDefinition()).matchDataTypeArguments(args1);
            if (args1 != null) {
              result = CheckTypeVisitor.DefCallResult.makeTResult(defCallResult.getDefCall(), defCallResult.getDefinition(), defCallResult.getSortArgument(), null);
              if (!args1.isEmpty()) {
                result = ((CheckTypeVisitor.DefCallResult) result).applyExpressions(args1);
              }
            }
          }
        }
      }
    }

    if (result == null) {
      myVisitor.checkExpr(arg, null);
      return null;
    }
    return inferArg(result, arg, isExplicit, fun);
  }

  private CheckTypeVisitor.TResult checkBinOpInferArg(Abstract.Expression fun, Abstract.Expression arg, boolean isExplicit, ExpectedType expectedType) {
    if (fun instanceof Abstract.BinOpExpression) {
      return inferArg(inferArg(inferArg(fun, ((Abstract.BinOpExpression) fun).getLeft(), true, null), ((Abstract.BinOpExpression) fun).getRight(), true, fun), arg, isExplicit, fun);
    } else {
      return inferArg(fun, arg, isExplicit, expectedType);
    }
  }

  @Override
  public CheckTypeVisitor.TResult infer(Abstract.AppExpression expr, ExpectedType expectedType) {
    Abstract.Argument arg = expr.getArgument();
    return checkBinOpInferArg(expr.getFunction(), arg.getExpression(), arg.isExplicit(), expectedType);
  }

  @Override
  public CheckTypeVisitor.TResult infer(Abstract.BinOpExpression expr, ExpectedType expectedType) {
    return inferArg(inferArg(expr, expr.getLeft(), true, null), expr.getRight(), true, expr);
  }

  @Override
  public CheckTypeVisitor.TResult inferTail(CheckTypeVisitor.TResult result, ExpectedType expectedType, Abstract.Expression expr) {
    List<? extends DependentLink> actualParams = result.getImplicitParameters();
    List<SingleDependentLink> expectedParams = new ArrayList<>(actualParams.size());
    expectedType.getPiParameters(expectedParams, true);
    if (expectedParams.size() > actualParams.size()) {
      CheckTypeVisitor.Result result1 = result.toResult(myVisitor.getEquations());
      LocalTypeCheckingError error = new TypeMismatchError(expectedType, result1.type, expr);
      expr.setWellTyped(myVisitor.getContext(), new ErrorExpression(result1.expression, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }

    if (expectedParams.size() != actualParams.size()) {
      result = fixImplicitArgs(result, actualParams.subList(0, actualParams.size() - expectedParams.size()), expr);
    }

    return result;
  }
}
