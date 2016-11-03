package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.term.*;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.FunctionInferenceVariable;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.TypeClassInferenceVariable;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.TypeMismatchError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class StdImplicitArgsInference extends BaseImplicitArgsInference {
  public StdImplicitArgsInference(CheckTypeVisitor visitor) {
    super(visitor);
  }

  protected boolean fixImplicitArgs(CheckTypeVisitor.PreResult result, int numParams, Abstract.Expression expr) {
    ExprSubstitution substitution = new ExprSubstitution();
    for (int i = 0; i < numParams; i++) {
      DependentLink parameter = result.getParameters().get(0);
      Type type = parameter.getType().subst(substitution, new LevelSubstitution()).normalize(NormalizeVisitor.Mode.WHNF);
      Expression typeExpr = type.toExpression();
      InferenceVariable infVar;
      if (typeExpr != null && typeExpr.toClassCall() != null && typeExpr.toClassCall() instanceof ClassViewCallExpression) {
        infVar = new TypeClassInferenceVariable(parameter.getName(), typeExpr, ((ClassViewCallExpression) typeExpr.toClassCall()).getClassView(), false, expr);
      } else {
        infVar = new FunctionInferenceVariable(parameter.getName(), type, i + 1, expr);
      }
      Expression binding = new InferenceReferenceExpression(infVar, myVisitor.getEquations());
      result.applyExpressions(Collections.singletonList(binding));
      substitution.add(parameter, binding);
    }
    return true;
  }

  protected CheckTypeVisitor.PreResult inferArg(CheckTypeVisitor.PreResult result, Abstract.Expression arg, boolean isExplicit, Abstract.Expression fun) {
    if (result == null) {
      return null;
    }

    if (isExplicit) {
      ConCallExpression conCall = result.getExpression().toConCall();
      if (conCall != null && conCall.getDefinition() == Prelude.PATH_CON && conCall.getDataTypeArguments().isEmpty()) {
        List<DependentLink> pathParams = new ArrayList<>();
        Prelude.PATH_CON.getTypeWithParams(pathParams, conCall.getPolyArguments());
        DependentLink lamParam = param("i", Interval());
        Expression binding = new InferenceReferenceExpression(new FunctionInferenceVariable("A", pathParams.get(0).getType().getPiCodomain(), 1, fun), myVisitor.getEquations());
        Expression lamExpr = Lam(lamParam, binding);
        result.applyExpressions(Collections.singletonList(lamExpr));

        CheckTypeVisitor.Result argResult = myVisitor.typeCheck(arg, Pi(lamParam, binding));
        if (argResult == null) {
          return null;
        }

        Expression expr1 = argResult.getExpression().addArgument(Left());
        Expression expr2 = argResult.getExpression().addArgument(Right());
        result.applyExpressions(Arrays.asList(expr1, expr2, argResult.getExpression()));
        return result;
      }

      List<DependentLink> params = new ArrayList<>();
      result.getImplicitParameters(params);
      if (!fixImplicitArgs(result, params.size(), fun)) {
        return null;
      }
    }

    DependentLink param = result.getParameters().isEmpty() ? EmptyDependentLink.getInstance() : result.getParameters().get(0);
    if (!param.hasNext()) {
      LocalTypeCheckingError error = new TypeMismatchError(new StringPrettyPrintable("A pi type"), result.getAtomicType(), fun);
      fun.setWellTyped(myVisitor.getContext(), new ErrorExpression(result.getExpression(), error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }

    CheckTypeVisitor.Result argResult = myVisitor.typeCheck(arg, param.getType());
    if (argResult == null) {
      return null;
    }

    if (param.isExplicit() != isExplicit) {
      LocalTypeCheckingError error = new LocalTypeCheckingError("Expected an " + (param.isExplicit() ? "explicit" : "implicit") + " argument", arg);
      arg.setWellTyped(myVisitor.getContext(), new ErrorExpression(argResult.getExpression(), error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }

    result.applyExpressions(Collections.singletonList(argResult.getExpression()));
    return result;
  }

  protected CheckTypeVisitor.PreResult inferArg(Abstract.Expression fun, Abstract.Expression arg, boolean isExplicit, Type expectedType) {
    CheckTypeVisitor.PreResult result;
    if (fun instanceof Abstract.AppExpression) {
      Abstract.ArgumentExpression argument = ((Abstract.AppExpression) fun).getArgument();
      result = inferArg(((Abstract.AppExpression) fun).getFunction(), argument.getExpression(), argument.isExplicit(), expectedType);
    } else {
      if (fun instanceof Abstract.DefCallExpression || fun instanceof Abstract.ApplyLevelExpression) {
        if (fun instanceof Abstract.DefCallExpression) {
          result = myVisitor.getTypeCheckingDefCall().typeCheckDefCall((Abstract.DefCallExpression) fun);
        } else {
          result = myVisitor.getTypeCheckingDefCall().typeCheckDefCall((Abstract.ApplyLevelExpression) fun);
        }
      //  if (result != null) {
      //    fun.setWellTyped(myVisitor.getContext(), result.getExpression());
      //  }
      } else {
        result = myVisitor.typeCheck(fun, null);
      }

      if (isExplicit && result != null) {
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
              Expression conCallUpdated = ConCall(conCall.getDefinition(), conCall.getPolyArguments(), new ArrayList<Expression>(), new ArrayList<Expression>());
              List<DependentLink> params = new ArrayList<>();
              Expression type = conCall.getDefinition().getTypeWithParams(params, conCall.getPolyArguments());
              result = new CheckTypeVisitor.PreResult(conCallUpdated, type, params);
              result.applyExpressions(args1);
            }
            return inferArg(result, arg, true, fun);
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

  @Override
  public CheckTypeVisitor.PreResult infer(Abstract.AppExpression expr, Type expectedType) {
    Abstract.ArgumentExpression arg = expr.getArgument();
    return inferArg(expr.getFunction(), arg.getExpression(), arg.isExplicit(), expectedType);
  }

  @Override
  public CheckTypeVisitor.PreResult infer(Abstract.BinOpExpression expr, Type expectedType) {
    Concrete.Position position = expr instanceof Concrete.Expression ? ((Concrete.Expression) expr).getPosition() : ConcreteExpressionFactory.POSITION;
    return inferArg(inferArg(new Concrete.DefCallExpression(position, expr.getResolvedBinOp()), expr.getLeft(), true, null), expr.getRight(), true, expr);
  }

  @Override
  public CheckTypeVisitor.Result inferTail(CheckTypeVisitor.Result result, Type expectedType, Abstract.Expression expr) {
    List<DependentLink> actualParams = new ArrayList<>();
    List<DependentLink> expectedParams = new ArrayList<>(actualParams.size());
    Type expectedType1 = expectedType.getPiParameters(expectedParams, true, true);
    result.getImplicitParameters(actualParams);
    if (expectedParams.size() > actualParams.size()) {
      LocalTypeCheckingError error = new TypeMismatchError(expectedType1.fromPiParameters(expectedParams), result.getType(), expr);
      expr.setWellTyped(myVisitor.getContext(), new ErrorExpression(result.getExpression(), error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }

    if (expectedParams.size() != actualParams.size()) {
      int argsNumber = actualParams.size() - expectedParams.size();
      if (!fixImplicitArgs(result, argsNumber, expr)) {
        return null;
      }
      expectedType = expectedType1.fromPiParameters(expectedParams); // TODO: do we need this line?
    }

    return myVisitor.checkResult(expectedType, result, expr);
  }
}
