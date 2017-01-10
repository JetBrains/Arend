package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelBinding;
import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.FunctionInferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.TypeClassInferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.expr.*;
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

  protected void fixImplicitArgs(CheckTypeVisitor.TResult result, List<DependentLink> implicitParameters, Abstract.Expression expr) {
    ExprSubstitution substitution = new ExprSubstitution();
    int i = 1;
    for (DependentLink parameter : implicitParameters) {
      Type type = parameter.getType().subst(substitution, new LevelSubstitution()).normalize(NormalizeVisitor.Mode.WHNF);
      Expression typeExpr = type.toExpression();
      InferenceVariable infVar;
      if (typeExpr != null && typeExpr.toClassCall() != null && typeExpr.toClassCall() instanceof ClassViewCallExpression) {
        infVar = new TypeClassInferenceVariable(parameter.getName(), typeExpr, null, (ClassField) myVisitor.getTypecheckingState().getTypechecked(((ClassViewCallExpression) typeExpr.toClassCall()).getClassView().getClassifyingField()), expr);
      } else {
        infVar = new FunctionInferenceVariable(parameter.getName(), type, i, expr);
      }
      Expression binding = new InferenceReferenceExpression(infVar, myVisitor.getEquations());
      result.applyExpressions(Collections.singletonList(binding));
      substitution.add(parameter, binding);
      i++;
    }
  }

  protected boolean inferArg(CheckTypeVisitor.TResult result, Abstract.Expression arg, boolean isExplicit, Abstract.Expression fun) {
    if (result == null) {
      return false;
    }

    if (isExplicit) {
      ConCallExpression conCall = result.getExpression().toConCall();
      if (conCall != null && conCall.getDefinition() == Prelude.PATH_CON && conCall.getDataTypeArguments().isEmpty()) {
        List<DependentLink> pathParams = new ArrayList<>();
        Prelude.PATH_CON.getTypeWithParams(pathParams, conCall.getPolyArguments());
        DependentLink lamParam = param("i", Interval());
        Expression binding = new InferenceReferenceExpression(new FunctionInferenceVariable("A", pathParams.get(0).getType().getPiCodomain(), 1, fun), myVisitor.getEquations());
        result.applyExpressions(Collections.singletonList(Lam(lamParam, binding)));

        CheckTypeVisitor.Result argResult = myVisitor.typeCheck(arg, Pi(lamParam, binding));
        if (argResult == null) {
          return false;
        }

        Expression expr1 = argResult.getExpression().addArgument(Left());
        Expression expr2 = argResult.getExpression().addArgument(Right());
        result.applyExpressions(Arrays.asList(expr1, expr2, argResult.getExpression()));
        return true;
      }

      fixImplicitArgs(result, result.getImplicitParameters(), fun);
    } else {
      DefCallExpression defCall = result.getExpression().toDefCall();
      if (result instanceof CheckTypeVisitor.DefCallResult && defCall != null) {
        CheckTypeVisitor.DefCallResult defCallResult = (CheckTypeVisitor.DefCallResult) result;
        List<Integer> userPolyParams = LevelBinding.getSublistOfUserBindings(defCall.getDefinition().getPolyParams());
        int numLevelArgs = defCallResult.getNumberOfLevels();
        if (numLevelArgs < userPolyParams.size()) {
          LevelBinding param = defCall.getDefinition().getPolyParams().get(userPolyParams.get(numLevelArgs));
          Level level = null;
          if (!(arg instanceof Abstract.InferHoleExpression)) {
            level = myVisitor.typeCheckLevel(arg, param.getType() == LevelVariable.LvlType.HLVL ? -1 : 0);
            if (level == null) {
              return false;
            }
          }
          defCallResult.applyLevels(level);
          if (level != null) {
            Level oldValue = defCall.getPolyArguments().getLevels().get(userPolyParams.get(numLevelArgs));
            if (oldValue == null) {
              assert false;
              return false;
            }
            myVisitor.getEquations().add(oldValue, level, Equations.CMP.EQ, fun);
          }
          return true;
        }
      }
    }

    DependentLink param = result.getParameter();
    if (!param.hasNext()) {
      LocalTypeCheckingError error = new TypeMismatchError(new StringPrettyPrintable("A pi type"), result.getType(), fun);
      fun.setWellTyped(myVisitor.getContext(), new ErrorExpression(result.getExpression(), error));
      myVisitor.getErrorReporter().report(error);
      return false;
    }

    CheckTypeVisitor.Result argResult = myVisitor.typeCheck(arg, param.getType());
    if (argResult == null) {
      return false;
    }

    if (param.isExplicit() != isExplicit) {
      LocalTypeCheckingError error = new LocalTypeCheckingError("Expected an " + (param.isExplicit() ? "explicit" : "implicit") + " argument", arg);
      arg.setWellTyped(myVisitor.getContext(), new ErrorExpression(argResult.getExpression(), error));
      myVisitor.getErrorReporter().report(error);
      return false;
    }

    result.applyExpressions(Collections.singletonList(argResult.getExpression()));
    return true;
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

      if (result != null) {
        if (isExplicit) {
          ConCallExpression conCall = result.getExpression().toConCall();
          if (conCall != null &&
            conCall.getDataTypeArguments().size() < DependentLink.Helper.size(conCall.getDefinition().getDataTypeParameters()) &&
            expectedType != null &&
            !conCall.getDefinition().typeHasErrors()) {
            Expression exprExpType = expectedType.toExpression();
            DataCallExpression dataCall = exprExpType == null ? null : exprExpType.normalize(NormalizeVisitor.Mode.WHNF).toDataCall();
            if (dataCall != null) {
              List<? extends Expression> args = dataCall.getDefCallArguments();
              List<Expression> args1 = new ArrayList<>(args.size());
              args1.addAll(conCall.getDataTypeArguments());
              args1.addAll(args.subList(conCall.getDataTypeArguments().size(), args.size()));
              args1 = conCall.getDefinition().matchDataTypeArguments(args1);
              if (args1 != null && !args1.isEmpty()) {
                ConCallExpression conCallUpdated = ConCall(conCall.getDefinition(), conCall.getPolyArguments(), new ArrayList<Expression>(), new ArrayList<Expression>());
                result = new CheckTypeVisitor.DefCallResult(conCallUpdated, conCall.getPolyArguments());
                result.applyExpressions(args1);
              }
              return inferArg(result, arg, true, fun) ? result : null;
            }
          }
        }

      }
    }

    if (result == null) {
      myVisitor.typeCheck(arg, null);
      return null;
    }
    return inferArg(result, arg, isExplicit, fun) ? result : null;
  }

  private CheckTypeVisitor.TResult checkBinOpInferArg(Abstract.Expression fun, Abstract.Expression arg, boolean isExplicit, Type expectedType) {
    if (fun instanceof Abstract.BinOpExpression) {
      CheckTypeVisitor.TResult result = inferArg(fun, ((Abstract.BinOpExpression) fun).getLeft(), true, null);
      return inferArg(result, ((Abstract.BinOpExpression) fun).getRight(), true, fun) && inferArg(result, arg, isExplicit, fun) ? result : null;
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
    CheckTypeVisitor.TResult result = inferArg(expr, expr.getLeft(), true, null);
    return inferArg(result, expr.getRight(), true, expr) ? result : null;
  }

  @Override
  public boolean inferTail(CheckTypeVisitor.TResult result, Type expectedType, Abstract.Expression expr) {
    List<DependentLink> actualParams = result.getImplicitParameters();
    List<DependentLink> expectedParams = new ArrayList<>(actualParams.size());
    expectedType.getPiParameters(expectedParams, true, true);
    if (expectedParams.size() > actualParams.size()) {
      LocalTypeCheckingError error = new TypeMismatchError(expectedType, result.getType(), expr);
      expr.setWellTyped(myVisitor.getContext(), new ErrorExpression(result.getExpression(), error));
      myVisitor.getErrorReporter().report(error);
      return false;
    }

    if (expectedParams.size() != actualParams.size()) {
      fixImplicitArgs(result, actualParams.subList(0, actualParams.size() - expectedParams.size()), expr);
    }

    return true;
  }
}
