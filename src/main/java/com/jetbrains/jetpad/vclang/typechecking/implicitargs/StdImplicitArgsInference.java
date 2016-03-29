package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.term.*;
import com.jetbrains.jetpad.vclang.term.context.binding.FunctionInferenceBinding;
import com.jetbrains.jetpad.vclang.term.context.binding.IgnoreBinding;
import com.jetbrains.jetpad.vclang.term.context.binding.InferenceBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.definition.TypeUniverse;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeMismatchError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class StdImplicitArgsInference extends BaseImplicitArgsInference {
  public StdImplicitArgsInference(CheckTypeVisitor visitor) {
    super(visitor);
  }

  protected boolean fixImplicitArgs(CheckTypeVisitor.Result result, List<DependentLink> parameters, Abstract.Expression expr) {
    if (parameters.isEmpty()) {
      return true;
    }

    Substitution substitution = new Substitution();
    List<Expression> arguments = new ArrayList<>();
    List<EnumSet<AppExpression.Flag>> flags = new ArrayList<>();
    for (int i = 0; i < parameters.size(); i++) {
      DependentLink parameter = parameters.get(i);
      InferenceBinding inferenceBinding = new FunctionInferenceBinding(parameter.getName(), parameter.getType().subst(substitution), i + 1, expr);
      result.addUnsolvedVariable(inferenceBinding);
      Expression binding = Reference(inferenceBinding);
      arguments.add(binding);
      flags.add(EnumSet.noneOf(AppExpression.Flag.class));
      substitution.add(parameter, binding);
    }
    result.expression = Apps(result.expression, arguments, flags);
    result.type = result.type.subst(substitution);
    return true;
  }

  protected CheckTypeVisitor.Result inferArg(CheckTypeVisitor.Result result, Abstract.Expression arg, boolean isExplicit, Abstract.Expression fun) {
    if (result == null) {
      return null;
    }

    if (isExplicit) {
      ConCallExpression conCall = result.expression.getFunction().toConCall();
      if (conCall != null && Prelude.isPathCon(conCall.getDefinition())) {
        Expression interval = DataCall(Prelude.INTERVAL);
        CheckTypeVisitor.Result argResult = myVisitor.typeCheck(arg, Pi(interval, Reference(new IgnoreBinding(null, Universe()))));
        if (argResult == null) {
          return null;
        }
        PiExpression piType = argResult.type.normalize(NormalizeVisitor.Mode.WHNF).toPi();
        DependentLink params = piType.getParameters();
        UniverseExpression universe = piType.getCodomain().getType().toUniverse();
        if (universe == null || !(universe.getUniverse() instanceof TypeUniverse)) {
          return null;
        }
        Expression lvlExpr = ((TypeUniverse) universe.getUniverse()).getLevel().getValue();
        Expression lamExpr;
        if (params.getNext().hasNext()) {
          DependentLink lamParam = param("i", interval);
          lamExpr = Lam(lamParam, Pi(params.getNext(), piType.getCodomain()).subst(params, Reference(lamParam)));
        } else {
          lamExpr = Lam(params, piType.getCodomain());
        }
        Expression expr1 = Apps(argResult.expression, ConCall(Prelude.LEFT));
        Expression expr2 = Apps(argResult.expression, ConCall(Prelude.RIGHT));
        Constructor pathCon = conCall.getDefinition();
        Expression  pathCall = ConCall(pathCon)
            .addArgument(lvlExpr, EnumSet.noneOf(AppExpression.Flag.class))
            .addArgument(lamExpr, AppExpression.DEFAULT)
            .addArgument(expr1, AppExpression.DEFAULT)
            .addArgument(expr2, AppExpression.DEFAULT);
        argResult.expression = Apps(pathCall, argResult.expression);
        argResult.type = Apps(DataCall(pathCon.getDataType()).addArgument(lvlExpr, EnumSet.noneOf(AppExpression.Flag.class)), lamExpr, expr1, expr2);
        return argResult;
      }

      List<DependentLink> params = new ArrayList<>();
      result.type = result.type.getPiParameters(params, true, true);
      if (!fixImplicitArgs(result, params, fun)) {
        return null;
      }
    } else {
      result.type = result.type.normalize(NormalizeVisitor.Mode.WHNF);
    }

    PiExpression actualType = result.type.toPi();
    if (actualType == null) {
      TypeCheckingError error = new TypeMismatchError(new StringPrettyPrintable("A pi type"), result.type, fun);
      fun.setWellTyped(myVisitor.getContext(), new ErrorExpression(result.expression, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }

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

    result.expression = result.expression.addArgument(argResult.expression, isExplicit ? EnumSet.of(AppExpression.Flag.EXPLICIT, AppExpression.Flag.VISIBLE) : EnumSet.of(AppExpression.Flag.VISIBLE));
    result.type = actualType.applyExpressions(Collections.singletonList(argResult.expression));
    result.add(argResult);
    result.update();
    return result;
  }

  protected CheckTypeVisitor.Result inferArg(Abstract.Expression fun, Abstract.Expression arg, boolean isExplicit, Expression expectedType) {
    CheckTypeVisitor.Result result;
    if (fun instanceof Abstract.AppExpression) {
      Abstract.ArgumentExpression argument = ((Abstract.AppExpression) fun).getArgument();
      result = inferArg(((Abstract.AppExpression) fun).getFunction(), argument.getExpression(), argument.isExplicit(), expectedType);
    } else {
      if (fun instanceof Abstract.DefCallExpression) {
        result = myVisitor.getTypeCheckingDefCall().typeCheckDefCall((Abstract.DefCallExpression) fun);
        if (result != null) {
          fun.setWellTyped(myVisitor.getContext(), result.expression);
        }
      } else {
        result = myVisitor.typeCheck(fun, null);
      }

      if (isExplicit && result != null) {
        ConCallExpression conCall = result.expression.getFunction().toConCall();
        if (conCall != null &&
            result.expression.getArguments().size() < DependentLink.Helper.size(conCall.getDefinition().getDataType().getParameters()) &&
            expectedType != null &&
            !conCall.getDefinition().hasErrors()) {
          Expression expectedTypeNorm = expectedType.normalize(NormalizeVisitor.Mode.WHNF);
          List<? extends Expression> args = expectedTypeNorm.getArguments();
          if (expectedTypeNorm.getFunction().toDataCall() != null) {
            List<Expression> args1 = new ArrayList<>(args.size());
            args1.addAll(result.expression.getArguments());
            args1.addAll(args.subList(result.expression.getArguments().size(), args.size()));
            args = conCall.getDefinition().matchDataTypeArguments(args1);
            if (!conCall.getDataTypeArguments().isEmpty()) {
              args = args.subList(conCall.getDataTypeArguments().size(), args.size());
            }
            if (!args.isEmpty()) {
              result.expression = Apps(result.expression, args, Collections.nCopies(args.size(), EnumSet.noneOf(AppExpression.Flag.class)));
              result.type = result.type.applyExpressions(args);
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
  public CheckTypeVisitor.Result infer(Abstract.AppExpression expr, Expression expectedType) {
    Abstract.ArgumentExpression arg = expr.getArgument();
    return inferArg(expr.getFunction(), arg.getExpression(), arg.isExplicit(), expectedType);
  }

  @Override
  public CheckTypeVisitor.Result infer(Abstract.BinOpExpression expr, Expression expectedType) {
    Concrete.Position position = expr instanceof Concrete.Expression ? ((Concrete.Expression) expr).getPosition() : ConcreteExpressionFactory.POSITION;
    return inferArg(inferArg(new Concrete.DefCallExpression(position, expr.getResolvedBinOp()), expr.getLeft(), true, null), expr.getRight(), true, expr);
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

    if (expectedParams.size() != actualParams.size()) {
      int argsNumber = actualParams.size() - expectedParams.size();
      result.type = actualType.fromPiParameters(actualParams.subList(argsNumber, actualParams.size()));
      if (!fixImplicitArgs(result, actualParams.subList(0, argsNumber), expr)) {
        return null;
      }
      expectedType = expectedType1.fromPiParameters(expectedParams);
    }

    result = myVisitor.checkResult(expectedType, result, expr);
    if (result != null) {
      result.update();
    }
    return result;
  }
}
