package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelBinding;
import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.FunctionInferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.TypeClassInferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.ErrorExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.InferenceReferenceExpression;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.prettyprint.StringPrettyPrintable;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.TypeMismatchError;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;

public class StdImplicitArgsInference extends BaseImplicitArgsInference {
  public StdImplicitArgsInference(CheckTypeVisitor visitor) {
    super(visitor);
  }

  protected CheckTypeVisitor.TResult fixImplicitArgs(CheckTypeVisitor.TResult result, List<DependentLink> implicitParameters, Abstract.Expression expr) {
    ExprSubstitution substitution = new ExprSubstitution();
    int i = 0;
    for (DependentLink parameter : implicitParameters) {
      Type type = parameter.getType().subst(substitution, new LevelSubstitution()).normalize(NormalizeVisitor.Mode.WHNF);
      Expression typeExpr = type.toExpression();
      InferenceVariable infVar = null;
      if (result instanceof CheckTypeVisitor.DefCallResult) {
        CheckTypeVisitor.DefCallResult defCallResult = (CheckTypeVisitor.DefCallResult) result;
        ClassField classifyingField = defCallResult.getDefinition().getClassifyingFieldOfParameter(defCallResult.getArguments().size());
        if (classifyingField != null) {
          infVar = new TypeClassInferenceVariable(parameter.getName(), typeExpr, defCallResult.getDefCall(), i, null, classifyingField);
        }
      }
      if (infVar == null) {
        infVar = new FunctionInferenceVariable(parameter.getName(), type, i + 1, result instanceof CheckTypeVisitor.DefCallResult ? ((CheckTypeVisitor.DefCallResult) result).getDefinition() : null, expr);
      }
      Expression binding = new InferenceReferenceExpression(infVar, myVisitor.getEquations());
      result = result.applyExpressions(Collections.singletonList(binding));
      substitution.add(parameter, binding);
      i++;
    }
    return result;
  }

  protected CheckTypeVisitor.TResult inferArg(CheckTypeVisitor.TResult result, Abstract.Expression arg, boolean isExplicit, Abstract.Expression fun) {
    if (result == null) {
      return null;
    }

    if (isExplicit) {
      if (result instanceof CheckTypeVisitor.DefCallResult) {
        CheckTypeVisitor.DefCallResult defCallResult = (CheckTypeVisitor.DefCallResult) result;
        if (defCallResult.getDefinition() == Prelude.PATH_CON && defCallResult.getArguments().isEmpty()) {
          List<DependentLink> pathParams = new ArrayList<>();
          Prelude.PATH_CON.getTypeWithParams(pathParams, defCallResult.getPolyArguments());
          DependentLink lamParam = param("i", Interval());
          Expression binding = new InferenceReferenceExpression(new FunctionInferenceVariable("A", pathParams.get(0).getType().getPiCodomain(), 1, Prelude.PATH_CON, fun), myVisitor.getEquations());
          result = result.applyExpressions(Collections.singletonList(Lam(lamParam, binding)));

          CheckTypeVisitor.Result argResult = myVisitor.typeCheck(arg, Pi(lamParam, binding));
          if (argResult == null) {
            return null;
          }

          Expression expr1 = argResult.expression.addArgument(Left());
          Expression expr2 = argResult.expression.addArgument(Right());
          return result.applyExpressions(Arrays.asList(expr1, expr2, argResult.expression));
        }
      }

      result = fixImplicitArgs(result, result.getImplicitParameters(), fun);
    } else if (result instanceof CheckTypeVisitor.DefCallResult) {
      CheckTypeVisitor.DefCallResult defCallResult = (CheckTypeVisitor.DefCallResult) result;
      List<Integer> userPolyParams = LevelBinding.getSublistOfUserBindings(defCallResult.getDefinition().getPolyParams());
      int numLevelArgs = defCallResult.getNumberOfLevels();
      if (numLevelArgs < userPolyParams.size()) {
        LevelBinding param = defCallResult.getDefinition().getPolyParams().get(userPolyParams.get(numLevelArgs));
        Level level = null;
        if (!(arg instanceof Abstract.InferHoleExpression)) {
          level = myVisitor.typeCheckLevel(arg, param.getType() == LevelVariable.LvlType.HLVL ? -1 : 0);
          if (level == null) {
            return null;
          }
        }
        defCallResult.applyLevels(level);
        if (level != null) {
          Level oldValue = defCallResult.getPolyArguments().getLevels().get(userPolyParams.get(numLevelArgs));
          if (oldValue == null) {
            assert false;
            return null;
          }
          myVisitor.getEquations().add(oldValue, level, Equations.CMP.EQ, fun);
        }
        return result;
      }
    }

    DependentLink param = result.getParameter();
    if (!param.hasNext()) {
      CheckTypeVisitor.Result result1 = result.toResult(myVisitor.getEquations(), fun);
      LocalTypeCheckingError error = new TypeMismatchError(new StringPrettyPrintable("A pi type"), result1.type, fun);
      fun.setWellTyped(myVisitor.getContext(), new ErrorExpression(result1.expression, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }

    CheckTypeVisitor.Result argResult = myVisitor.typeCheck(arg, param.getType());
    if (argResult == null) {
      return null;
    }

    if (param.isExplicit() != isExplicit) {
      LocalTypeCheckingError error = new LocalTypeCheckingError("Expected an " + (param.isExplicit() ? "explicit" : "implicit") + " argument", arg);
      arg.setWellTyped(myVisitor.getContext(), new ErrorExpression(argResult.expression, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }

    return result.applyExpressions(Collections.singletonList(argResult.expression));
  }

  protected CheckTypeVisitor.TResult inferArg(Abstract.Expression fun, Abstract.Expression arg, boolean isExplicit, Type expectedType) {
    CheckTypeVisitor.TResult result;
    if (fun instanceof Abstract.AppExpression) {
      Abstract.ArgumentExpression argument = ((Abstract.AppExpression) fun).getArgument();
      result = checkBinOpInferArg(((Abstract.AppExpression) fun).getFunction(), argument.getExpression(), argument.isExplicit(), expectedType);
    } else {
      if (fun instanceof Abstract.DefCallExpression) {
        Abstract.DefCallExpression defCall = (Abstract.DefCallExpression) fun;
        result = defCall.getExpression() == null && defCall.getReferent() == null ? myVisitor.getLocalVar(defCall) : myVisitor.getTypeCheckingDefCall().typeCheckDefCall(defCall);
      } else {
        result = myVisitor.typeCheck(fun, null);
      }

      if (result instanceof CheckTypeVisitor.DefCallResult && isExplicit && expectedType != null) {
        CheckTypeVisitor.DefCallResult defCallResult = (CheckTypeVisitor.DefCallResult) result;
        if (defCallResult.getDefinition() instanceof Constructor && defCallResult.getArguments().size() < DependentLink.Helper.size(((Constructor) defCallResult.getDefinition()).getDataTypeParameters())) {
          Expression exprExpType = expectedType.toExpression();
          DataCallExpression dataCall = exprExpType == null ? null : exprExpType.normalize(NormalizeVisitor.Mode.WHNF).toDataCall();
          if (dataCall != null) {
            List<? extends Expression> args = dataCall.getDefCallArguments();
            List<Expression> args1 = new ArrayList<>(args.size());
            args1.addAll(defCallResult.getArguments());
            args1.addAll(args.subList(defCallResult.getArguments().size(), args.size()));
            args1 = ((Constructor) defCallResult.getDefinition()).matchDataTypeArguments(args1);
            if (args1 != null) {
              result = CheckTypeVisitor.DefCallResult.makeTResult(defCallResult.getDefCall(), defCallResult.getDefinition(), defCallResult.getPolyArguments(), null).applyExpressions(args1);
            }
          }
        }
      }
    }

    if (result == null) {
      myVisitor.typeCheck(arg, null);
      return null;
    }
    return inferArg(result, arg, isExplicit, fun);
  }

  private CheckTypeVisitor.TResult checkBinOpInferArg(Abstract.Expression fun, Abstract.Expression arg, boolean isExplicit, Type expectedType) {
    if (fun instanceof Abstract.BinOpExpression) {
      return inferArg(inferArg(inferArg(fun, ((Abstract.BinOpExpression) fun).getLeft(), true, null), ((Abstract.BinOpExpression) fun).getRight(), true, fun), arg, isExplicit, fun);
    } else {
      return inferArg(fun, arg, isExplicit, expectedType);
    }
  }

  @Override
  public CheckTypeVisitor.TResult infer(Abstract.AppExpression expr, Type expectedType) {
    Abstract.ArgumentExpression arg = expr.getArgument();
    return checkBinOpInferArg(expr.getFunction(), arg.getExpression(), arg.isExplicit(), expectedType);
  }

  @Override
  public CheckTypeVisitor.TResult infer(Abstract.BinOpExpression expr, Type expectedType) {
    return inferArg(inferArg(expr, expr.getLeft(), true, null), expr.getRight(), true, expr);
  }

  @Override
  public CheckTypeVisitor.TResult inferTail(CheckTypeVisitor.TResult result, Type expectedType, Abstract.Expression expr) {
    List<DependentLink> actualParams = result.getImplicitParameters();
    List<DependentLink> expectedParams = new ArrayList<>(actualParams.size());
    expectedType.getPiParameters(expectedParams, true, true);
    if (expectedParams.size() > actualParams.size()) {
      CheckTypeVisitor.Result result1 = result.toResult(myVisitor.getEquations(), expr);
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
