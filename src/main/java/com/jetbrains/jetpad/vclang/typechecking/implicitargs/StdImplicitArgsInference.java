package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.core.context.binding.inference.FunctionInferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.TypeClassInferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.TypedSingleDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.type.ExpectedType;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.naming.reference.TCClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.TCReferable;
import com.jetbrains.jetpad.vclang.prelude.Prelude;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.error.local.NotPiType;
import com.jetbrains.jetpad.vclang.typechecking.error.local.TypeMismatchError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.TypecheckingError;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.refDoc;

public class StdImplicitArgsInference extends BaseImplicitArgsInference {
  public StdImplicitArgsInference(CheckTypeVisitor visitor) {
    super(visitor);
  }

  private static TCClassReferable getClassRefFromDefCall(Definition definition, int paramIndex) {
    if (definition instanceof ClassField) {
      return paramIndex == 0 ? ((ClassField) definition).getParentClass().getReferable() : null;
    }

    int i = 0;
    DependentLink link;
    if (definition instanceof Constructor) {
      link = ((Constructor) definition).getDataTypeParameters();
      while (link.hasNext() && i < paramIndex) {
        link = link.getNext();
        i++;
      }
      if (i < paramIndex) {
        link = definition.getParameters();
      }
    } else {
      link = definition.getParameters();
    }

    while (i < paramIndex && link.hasNext()) {
      link = link.getNext();
      i++;
    }

    if (!link.hasNext()) {
      return null;
    }

    ClassCallExpression type = link.getTypeExpr().checkedCast(ClassCallExpression.class);
    return type != null ? type.getDefinition().getReferable() : null;
  }

  private CheckTypeVisitor.TResult fixImplicitArgs(CheckTypeVisitor.TResult result, List<? extends DependentLink> implicitParameters, Concrete.Expression expr, boolean classVarsOnly) {
    ExprSubstitution substitution = new ExprSubstitution();
    int i = 0;
    for (DependentLink parameter : implicitParameters) {
      Expression type = parameter.getTypeExpr().subst(substitution, LevelSubstitution.EMPTY);
      InferenceVariable infVar = null;
      if (result instanceof CheckTypeVisitor.DefCallResult) {
        CheckTypeVisitor.DefCallResult defCallResult = (CheckTypeVisitor.DefCallResult) result;
        TCClassReferable classRef = getClassRefFromDefCall(defCallResult.getDefinition(), i);
        if (classRef != null) {
          infVar = new TypeClassInferenceVariable(parameter.getName(), type, classRef, defCallResult.getDefCall(), myVisitor.getAllBindings());
        }
      }
      if (infVar == null) {
        if (classVarsOnly) {
          return result;
        }
        if (result instanceof CheckTypeVisitor.DefCallResult) {
          infVar = new FunctionInferenceVariable(parameter.getName(), type, ((CheckTypeVisitor.DefCallResult) result).getArguments().size() + 1, ((CheckTypeVisitor.DefCallResult) result).getDefinition(), expr, myVisitor.getAllBindings());
        } else {
          infVar = new FunctionInferenceVariable(parameter.getName(), type, i + 1, null, expr, myVisitor.getAllBindings());
        }
      }
      Expression binding = new InferenceReferenceExpression(infVar, myVisitor.getEquations());
      result = result.applyExpression(binding, myVisitor.getErrorReporter(), expr);
      substitution.add(parameter, binding);
      i++;
    }
    return result;
  }

  private CheckTypeVisitor.TResult inferArg(CheckTypeVisitor.TResult result, Concrete.Expression arg, boolean isExplicit, Concrete.Expression fun) {
    if (result == null || arg == null || result instanceof CheckTypeVisitor.Result && ((CheckTypeVisitor.Result) result).expression.isError()) {
      myVisitor.checkExpr(arg, null);
      return result;
    }

    if (isExplicit) {
      if (result instanceof CheckTypeVisitor.DefCallResult) {
        CheckTypeVisitor.DefCallResult defCallResult = (CheckTypeVisitor.DefCallResult) result;
        if (defCallResult.getDefinition() == Prelude.PATH_CON && defCallResult.getArguments().isEmpty()) {
          SingleDependentLink lamParam = new TypedSingleDependentLink(true, "i", Interval());
          UniverseExpression type = new UniverseExpression(new Sort(defCallResult.getSortArgument().getPLevel(), defCallResult.getSortArgument().getHLevel().add(1)));
          Expression binding = new InferenceReferenceExpression(new FunctionInferenceVariable("A", type, 1, Prelude.PATH_CON, fun, myVisitor.getAllBindings()), myVisitor.getEquations());
          Sort sort = type.getSort().succ();
          result = result.applyExpression(new LamExpression(sort, lamParam, binding), myVisitor.getErrorReporter(), fun);

          CheckTypeVisitor.Result argResult = myVisitor.checkExpr(arg, new PiExpression(sort, lamParam, binding));
          if (argResult == null) {
            return null;
          }

          Expression expr1 = new AppExpression(argResult.expression, Left());
          Expression expr2 = new AppExpression(argResult.expression, Right());
          return ((CheckTypeVisitor.DefCallResult) result).applyExpressions(Arrays.asList(expr1, expr2, argResult.expression));
        }
      }

      result = fixImplicitArgs(result, result.getImplicitParameters(), fun, false);
    }

    DependentLink param = result.getParameter();
    CheckTypeVisitor.Result argResult = myVisitor.checkExpr(arg, param.hasNext() ? param.getTypeExpr() : null);
    if (argResult == null) {
      return null;
    }

    if (!param.hasNext()) {
      CheckTypeVisitor.Result result1 = result.toResult(myVisitor.getEquations());
      if (!result1.type.isError()) {
        myVisitor.getErrorReporter().report(new NotPiType(argResult.expression, result1.type, fun));
      }
      return null;
    }

    if (param.isExplicit() != isExplicit) {
      myVisitor.getErrorReporter().report(new TypecheckingError("Expected an " + (param.isExplicit() ? "explicit" : "implicit") + " argument", arg));
      return null;
    }

    return result.applyExpression(argResult.expression, myVisitor.getErrorReporter(), fun);
  }

  @Override
  public CheckTypeVisitor.TResult infer(Concrete.AppExpression expr, ExpectedType expectedType) {
    CheckTypeVisitor.TResult result;
    Concrete.Expression fun = expr.getFunction();
    if (fun instanceof Concrete.ReferenceExpression) {
      Concrete.ReferenceExpression defCall = (Concrete.ReferenceExpression) fun;
      result = defCall.getReferent() instanceof TCReferable ? myVisitor.getTypeCheckingDefCall().typeCheckDefCall((TCReferable) defCall.getReferent(), defCall) : myVisitor.getLocalVar(defCall);
    } else {
      result = myVisitor.checkExpr(fun, null);
    }

    if (result == null) {
      for (Concrete.Argument argument : expr.getArguments()) {
        myVisitor.checkExpr(argument.expression, null);
      }
      return null;
    }

    if (result instanceof CheckTypeVisitor.DefCallResult && expr.getArguments().get(0).isExplicit() && expectedType != null) {
      CheckTypeVisitor.DefCallResult defCallResult = (CheckTypeVisitor.DefCallResult) result;
      if (defCallResult.getDefinition() instanceof Constructor && defCallResult.getArguments().size() < DependentLink.Helper.size(((Constructor) defCallResult.getDefinition()).getDataTypeParameters())) {
        DataCallExpression dataCall = expectedType instanceof Expression ? ((Expression) expectedType).normalize(NormalizeVisitor.Mode.WHNF).checkedCast(DataCallExpression.class) : null;
        if (dataCall != null) {
          if (((Constructor) defCallResult.getDefinition()).getDataType() != dataCall.getDefinition()) {
            myVisitor.getErrorReporter().report(new TypeMismatchError(dataCall, refDoc(((Constructor) defCallResult.getDefinition()).getDataType().getReferable()), fun));
            return null;
          }

          List<? extends Expression> args = dataCall.getDefCallArguments();
          List<Expression> args1 = new ArrayList<>(args.size());
          args1.addAll(defCallResult.getArguments());
          args1.addAll(args.subList(defCallResult.getArguments().size(), args.size()));
          args1 = ((Constructor) defCallResult.getDefinition()).matchDataTypeArguments(args1);
          if (args1 != null) {
            result = CheckTypeVisitor.DefCallResult.makeTResult(defCallResult.getDefCall(), defCallResult.getDefinition(), defCallResult.getSortArgument());
            if (!args1.isEmpty()) {
              result = ((CheckTypeVisitor.DefCallResult) result).applyExpressions(args1);
            }
          }
        }
      }
    }

    for (Concrete.Argument argument : expr.getArguments()) {
      result = inferArg(result, argument.expression, argument.isExplicit(), fun);
    }
    return result;
  }

  @Override
  public CheckTypeVisitor.TResult inferTail(CheckTypeVisitor.TResult result, ExpectedType expectedType, Concrete.Expression expr) {
    List<? extends DependentLink> actualParams = result.getImplicitParameters();
    int expectedParamsNumber = 0;
    if (expectedType != null) {
      List<SingleDependentLink> expectedParams = new ArrayList<>(actualParams.size());
      expectedType.getPiParameters(expectedParams, true);
      if (expectedParams.size() > actualParams.size()) {
        CheckTypeVisitor.Result result1 = result.toResult(myVisitor.getEquations());
        if (!result1.type.isError()) {
          myVisitor.getErrorReporter().report(new TypeMismatchError(expectedType, result1.type, expr));
        }
        return null;
      }
      expectedParamsNumber = expectedParams.size();
    }

    if (expectedParamsNumber != actualParams.size()) {
      result = fixImplicitArgs(result, actualParams.subList(0, actualParams.size() - expectedParamsNumber), expr, expectedType == null);
    }

    return result;
  }
}
