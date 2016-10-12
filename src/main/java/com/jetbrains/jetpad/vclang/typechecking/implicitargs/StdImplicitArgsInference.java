package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.term.*;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.FunctionInferenceVariable;
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

  protected boolean fixImplicitArgs(CheckTypeVisitor.PreResult result, int num_params, Abstract.Expression expr) {
   // if (parameters.isEmpty()) {
   //   return true;
   // }

    // ExprSubstitution substitution = new ExprSubstitution();
    for (int i = 0; i < num_params; i++) {
      DependentLink parameter = result.getParameters().get(0);
      Expression binding = new InferenceReferenceExpression(new FunctionInferenceVariable(parameter.getName(), parameter.getType(), i + 1, expr));
      result.applyExpressions(Collections.singletonList(binding));
      //result.expression = result.expression.addArgument(binding);
     // substitution.add(parameter, binding);
    }
    //result.type = result.type.subst(substitution, new LevelSubstitution());
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
        //((Expression) conCall.getType()).getPiParameters(pathParams, false, false);
        Prelude.PATH_CON.getTypeWithParams(pathParams, conCall.getPolyParamsSubst());
        DependentLink lamParam = param("i", Interval());
        Expression binding = new InferenceReferenceExpression(new FunctionInferenceVariable("A", pathParams.get(0).getType().getPiCodomain().toExpression(), 1, fun)); // TODO: get rid of toExpression
        Expression lamExpr = Lam(lamParam, binding);
        // result.type = result.type.applyExpressions(Collections.singletonList(lamExpr));
        result.applyExpressions(Collections.singletonList(lamExpr));

        CheckTypeVisitor.Result argResult = myVisitor.typeCheck(arg, Pi(lamParam, binding));
        if (argResult == null) {
          return null;
        }

        Expression expr1 = argResult.getExpression().addArgument(Left());
        Expression expr2 = argResult.getExpression().addArgument(Right());
        result.applyExpressions(Arrays.asList(expr1, expr2, argResult.getExpression()));
        //result.expression = ConCall(conCall.getDefinition(), conCall.getPolyParamsSubst(), Arrays.asList(lamExpr, expr1, expr2), Collections.singletonList(argResult.expression));
        //result.type = result.type.applyExpressions(Arrays.asList(expr1, expr2, argResult.expression));
        return result;
      }

      List<DependentLink> params = new ArrayList<>();
      //result.type = result.type.getPiParameters(params, true, true);
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

    //result.expression = result.expression.addArgument(argResult.expression);
    //result.type = result.type.applyExpressions(Collections.singletonList(argResult.expression));
    result.applyExpressions(Collections.singletonList(argResult.getExpression()));
    return result;
  }

  protected CheckTypeVisitor.PreResult inferArg(Abstract.Expression fun, Abstract.Expression arg, boolean isExplicit, Expression expectedType) {
    CheckTypeVisitor.PreResult result;
    if (fun instanceof Abstract.AppExpression) {
      Abstract.ArgumentExpression argument = ((Abstract.AppExpression) fun).getArgument();
      result = inferArg(((Abstract.AppExpression) fun).getFunction(), argument.getExpression(), argument.isExplicit(), expectedType);
    } else {
      if (fun instanceof Abstract.DefCallExpression) {
        result = myVisitor.getTypeCheckingDefCall().typeCheckDefCall((Abstract.DefCallExpression) fun);
        if (result != null) {
          fun.setWellTyped(myVisitor.getContext(), result.getExpression());
        }
      } else {
        result = myVisitor.typeCheck(fun, null);
      }

      if (isExplicit && result != null) {
        ConCallExpression conCall = result.getExpression().toConCall();
        if (conCall != null &&
            conCall.getDataTypeArguments().size() < DependentLink.Helper.size(conCall.getDefinition().getDataTypeParameters()) &&
            expectedType != null &&
            !conCall.getDefinition().hasErrors()) {
          DataCallExpression dataCall = expectedType.normalize(NormalizeVisitor.Mode.WHNF).toDataCall();
          if (dataCall != null) {
            List<? extends Expression> args = dataCall.getDefCallArguments();
            List<Expression> args1 = new ArrayList<>(args.size());
            args1.addAll(conCall.getDataTypeArguments());
            args1.addAll(args.subList(conCall.getDataTypeArguments().size(), args.size()));
            args1 = conCall.getDefinition().matchDataTypeArguments(args1);
            if (args1 != null && !args1.isEmpty()) {
              Expression conCallUpdated = ConCall(conCall.getDefinition(), conCall.getPolyParamsSubst(), new ArrayList<Expression>(), new ArrayList<Expression>());
              List<DependentLink> params = new ArrayList<>();
              Expression type = conCall.getDefinition().getTypeWithParams(params, conCall.getPolyParamsSubst());
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
  public CheckTypeVisitor.PreResult infer(Abstract.AppExpression expr, Expression expectedType) {
    Abstract.ArgumentExpression arg = expr.getArgument();
    return inferArg(expr.getFunction(), arg.getExpression(), arg.isExplicit(), expectedType);
  }

  @Override
  public CheckTypeVisitor.PreResult infer(Abstract.BinOpExpression expr, Expression expectedType) {
    Concrete.Position position = expr instanceof Concrete.Expression ? ((Concrete.Expression) expr).getPosition() : ConcreteExpressionFactory.POSITION;
    return inferArg(inferArg(new Concrete.DefCallExpression(position, expr.getResolvedBinOp()), expr.getLeft(), true, null), expr.getRight(), true, expr);
  }

  @Override
  public CheckTypeVisitor.Result inferTail(CheckTypeVisitor.Result result, Expression expectedType, Abstract.Expression expr) {
    List<DependentLink> actualParams = new ArrayList<>();
    // Type actualType = result.type.getPiParameters(actualParams, true, true);
    List<DependentLink> expectedParams = new ArrayList<>(actualParams.size());
    Expression expectedType1 = expectedType.getPiParameters(expectedParams, true, true);
    result.getImplicitParameters(actualParams);
    if (expectedParams.size() > actualParams.size()) {
      // TODO: what if result.parameters contain parameter, which is not Expression?
      LocalTypeCheckingError error = new TypeMismatchError(expectedType1.fromPiParameters(expectedParams), result.getType(), expr);
      expr.setWellTyped(myVisitor.getContext(), new ErrorExpression(result.getExpression(), error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }

    if (expectedParams.size() != actualParams.size()) {
      int argsNumber = actualParams.size() - expectedParams.size();
      //result.type = actualType.fromPiParameters(actualParams.subList(argsNumber, actualParams.size()));
      if (!fixImplicitArgs(result, argsNumber, expr)) {
        return null;
      }
      expectedType = expectedType1.fromPiParameters(expectedParams); // TODO: do we need this line?
    }

    return myVisitor.checkResult(expectedType, result, expr);
  }
}
