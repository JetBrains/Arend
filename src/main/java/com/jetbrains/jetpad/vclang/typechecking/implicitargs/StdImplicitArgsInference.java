package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.core.context.binding.inference.ExpressionInferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.FunctionInferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.TypeClassInferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.TypedSingleDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.type.ExpectedType;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.naming.reference.LocatedReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.TCClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.TCReferable;
import com.jetbrains.jetpad.vclang.prelude.Prelude;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.error.local.*;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.*;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.refDoc;

public class StdImplicitArgsInference extends BaseImplicitArgsInference {
  public StdImplicitArgsInference(CheckTypeVisitor visitor) {
    super(visitor);
  }

  private static ClassDefinition getClassRefFromDefCall(Definition definition, int paramIndex) {
    if (definition instanceof ClassField) {
      return paramIndex == 0 ? ((ClassField) definition).getParentClass() : null;
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
    return type != null ? type.getDefinition() : null;
  }

  private static TCClassReferable getClassRefFromDefCall(Concrete.Expression expr, int paramIndex) {
    if (paramIndex != 0) {
      return null;
    }
    if (expr instanceof Concrete.AppExpression) {
      expr = ((Concrete.AppExpression) expr).getFunction();
    }
    if (!(expr instanceof Concrete.ReferenceExpression)) {
      return null;
    }

    Referable ref = ((Concrete.ReferenceExpression) expr).getReferent();
    if (ref instanceof LocatedReferable && ((LocatedReferable) ref).isFieldSynonym()) {
      ref = ((TCReferable) ref).getLocatedReferableParent();
      if (ref instanceof TCClassReferable) {
        return (TCClassReferable) ref;
      }
    }
    return null;
  }

  private CheckTypeVisitor.TResult fixImplicitArgs(CheckTypeVisitor.TResult result, List<? extends DependentLink> implicitParameters, Concrete.Expression expr, boolean classVarsOnly) {
    ExprSubstitution substitution = new ExprSubstitution();
    int i = result instanceof CheckTypeVisitor.DefCallResult ? ((CheckTypeVisitor.DefCallResult) result).getArguments().size() : 0;
    for (DependentLink parameter : implicitParameters) {
      Expression type = parameter.getTypeExpr().subst(substitution, LevelSubstitution.EMPTY);
      InferenceVariable infVar = null;

      // If result is defCall, then try to infer class instances.
      if (result instanceof CheckTypeVisitor.DefCallResult) {
        CheckTypeVisitor.DefCallResult defCallResult = (CheckTypeVisitor.DefCallResult) result;
        ClassDefinition classDef = getClassRefFromDefCall(defCallResult.getDefinition(), i);
        if (classDef != null && !classDef.isRecord()) {
          // If the class does not have a classifying field, infer instance immediately
          if (classDef.getClassifyingField() == null) {
            Expression instance = myVisitor.getInstancePool().getInstance(null, classDef.getReferable(), defCallResult.getDefinition() instanceof ClassField, myVisitor.getEquations(), expr);
            if (instance == null) {
              ArgInferenceError error = new InstanceInferenceError(classDef.getReferable(), expr, new Expression[0]);
              myVisitor.getErrorReporter().report(error);
              instance = new ErrorExpression(null, error);
            }
            result = result.applyExpression(instance, myVisitor.getErrorReporter(), expr);
            substitution.add(parameter, instance);
            i++;
            continue;
          }

          // Otherwise, generate type class inference variable
          boolean isField = true;
          TCClassReferable classRef = getClassRefFromDefCall(expr, i);
          if (classRef == null) {
            isField = defCallResult.getDefinition() instanceof ClassField;
            classRef = classDef.getReferable();
          }

          if (classRef != null) {
            infVar = new TypeClassInferenceVariable(parameter.getName(), type, classRef, isField, defCallResult.getDefCall(), myVisitor.getAllBindings());
          }
        }
      }

      // Generate ordinary inference variable
      if (infVar == null) {
        if (classVarsOnly) {
          return result;
        }
        infVar = new FunctionInferenceVariable(parameter.getName(), type, i + 1, result instanceof CheckTypeVisitor.DefCallResult ? ((CheckTypeVisitor.DefCallResult) result).getDefinition() : null, expr, myVisitor.getAllBindings());
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

          Expression expr1 = AppExpression.make(argResult.expression, Left());
          Expression expr2 = AppExpression.make(argResult.expression, Right());
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
      result = myVisitor.visitReference((Concrete.ReferenceExpression) fun);
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

    if (result instanceof CheckTypeVisitor.DefCallResult) {
      // Check for parameters that have pi type which we might not infer.
      // In this case, we defer typechecking of the corresponding argument.
      List<Pair<InferenceVariable,Concrete.Argument>> deferredArguments = null;
      for (Concrete.Argument argument : expr.getArguments()) {
        if (argument.isExplicit() && result instanceof CheckTypeVisitor.DefCallResult && ((CheckTypeVisitor.DefCallResult) result).getDefinition() != Prelude.PATH_CON) {
          result = fixImplicitArgs(result, result.getImplicitParameters(), fun, false);
        }
        if (result != null && shouldBeDeferred(argument, result)) {
          if (deferredArguments == null) {
            deferredArguments = new ArrayList<>();
          }
          InferenceVariable var = new ExpressionInferenceVariable(result.getParameter().getTypeExpr(), argument.expression, myVisitor.getAllBindings());
          deferredArguments.add(new Pair<>(var, argument));
          result = result.applyExpression(new InferenceReferenceExpression(var, myVisitor.getEquations()), myVisitor.getErrorReporter(), fun);
        } else {
          result = inferArg(result, argument.expression, argument.isExplicit(), fun);
        }
      }
      if (deferredArguments != null) {
        for (Pair<InferenceVariable, Concrete.Argument> pair : deferredArguments) {
          CheckTypeVisitor.Result argResult = myVisitor.checkExpr(pair.proj2.expression, pair.proj1.getType());
          Expression argResultExpr = argResult == null ? new ErrorExpression(null, null) : argResult.expression;
          pair.proj1.solve(myVisitor.getEquations(), argResultExpr);
        }
      }
    } else {
      for (Concrete.Argument argument : expr.getArguments()) {
        result = inferArg(result, argument.expression, argument.isExplicit(), fun);
      }
    }

    return result;
  }

  private boolean shouldBeDeferred(Concrete.Argument argument, CheckTypeVisitor.TResult result) {
    if (!(argument.getExpression() instanceof Concrete.LamExpression && result instanceof CheckTypeVisitor.DefCallResult)) {
      return false;
    }
    DependentLink param = argument.isExplicit() ? ((CheckTypeVisitor.DefCallResult) result).getExplicitParameter() : result.getParameter();
    if (!param.hasNext() || !argument.isExplicit() && param.isExplicit()) {
      return false;
    }

    // Collect inference variables that appear in pi parameters of the expected type of the argument.
    Set<InferenceVariable> suspiciousParameters = null;
    Expression type = param.getTypeExpr();
    while (type instanceof PiExpression) {
      Expression paramType = ((PiExpression) type).getParameters().getTypeExpr();
      if (paramType instanceof InferenceReferenceExpression) {
        InferenceVariable var = ((InferenceReferenceExpression) paramType).getVariable();
        boolean found = false;
        if (!var.isSolved()) {
          for (Expression argExpr : ((CheckTypeVisitor.DefCallResult) result).getArguments()) {
            if (argExpr instanceof InferenceReferenceExpression && ((InferenceReferenceExpression) argExpr).getVariable() == var) {
              found = true;
              break;
            }
          }
        }

        if (found) {
          if (suspiciousParameters == null) {
            suspiciousParameters = new HashSet<>();
          }
          suspiciousParameters.add(var);
        }
      }
      type = ((PiExpression) type).getCodomain();
    }

    if (suspiciousParameters == null) {
      return false;
    }

    List<DependentLink> parameters = new ArrayList<>();
    type = param.getTypeExpr();
    while (type instanceof PiExpression) {
      for (SingleDependentLink link = ((PiExpression) type).getParameters(); link.hasNext(); link = link.getNext()) {
        parameters.add(link);
      }
      type = ((PiExpression) type).getCodomain();
    }

    // If the type of a suspicious parameter is explicitly specified in the lambda of the argument, ignore it.
    int i = 0;
    Concrete.Expression argExpr = argument.expression;
    while (argExpr instanceof Concrete.LamExpression) {
      for (Concrete.Parameter parameter : ((Concrete.LamExpression) argExpr).getParameters()) {
        int n = parameter instanceof Concrete.TelescopeParameter ? ((Concrete.TelescopeParameter) parameter).getReferableList().size() : 1;
        for (; n > 0; n--) {
          // If we checked all parameters and didn't find problems, then everything is fine, return false.
          if (i >= parameters.size()) {
            return false;
          }
          // Skip implicit parameters of lambda.
          if (!parameter.getExplicit() && parameters.get(i).isExplicit()) {
            continue;
          }
          type = parameters.get(i).getTypeExpr();
          // If we found a suspicious parameter for which the type is not specified in lambda, then return true.
          if (type instanceof InferenceReferenceExpression &&
              suspiciousParameters.remove(((InferenceReferenceExpression) type).getVariable()) &&
              (parameter.getExplicit() != parameters.get(i).isExplicit() || !(parameter instanceof Concrete.TypeParameter))) {
            return true;
          }
          // Skip implicit parameters of pi.
          if (parameter.getExplicit() && !parameters.get(i).isExplicit()) {
            n++;
          }
          i++;
        }
      }
      argExpr = ((Concrete.LamExpression) argExpr).body;
    }

    return false;
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
