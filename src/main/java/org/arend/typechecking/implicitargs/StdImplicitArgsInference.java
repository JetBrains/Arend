package org.arend.typechecking.implicitargs;

import org.arend.core.context.binding.inference.ExpressionInferenceVariable;
import org.arend.core.context.binding.inference.FunctionInferenceVariable;
import org.arend.core.context.binding.inference.InferenceVariable;
import org.arend.core.context.binding.inference.TypeClassInferenceVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.context.param.TypedSingleDependentLink;
import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Constructor;
import org.arend.core.definition.Definition;
import org.arend.core.expr.*;
import org.arend.core.expr.type.ExpectedType;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.naming.reference.TCClassReferable;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.*;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.arend.util.Pair;

import java.util.*;

import static org.arend.core.expr.ExpressionFactory.*;
import static org.arend.error.doc.DocFactory.refDoc;

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
          TCClassReferable classRef = classDef.getReferable();
          if (defCallResult.getDefinition().isTypeClassParameter(i)) {
            // If the class does not have a classifying field, infer instance immediately
            if (classDef.getClassifyingField() == null) {
              Expression instance = myVisitor.getInstancePool().getInstance(null, classRef, myVisitor.getEquations(), expr);
              if (instance == null) {
                ArgInferenceError error = new InstanceInferenceError(classRef, expr, new Expression[0]);
                myVisitor.getErrorReporter().report(error);
                instance = new ErrorExpression(null, error);
              }
              result = result.applyExpression(instance, myVisitor.getErrorReporter(), expr);
              substitution.add(parameter, instance);
              i++;
              continue;
            }

            // Otherwise, generate type class inference variable
            infVar = new TypeClassInferenceVariable(parameter.getName(), type, classRef, defCallResult.getDefCall(), myVisitor.getAllBindings());
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
      myVisitor.checkArgument(arg, null, result);
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

          CheckTypeVisitor.Result argResult = myVisitor.checkArgument(arg, new PiExpression(sort, lamParam, binding), result);
          if (argResult == null) {
            return null;
          }

          Expression expr1 = AppExpression.make(argResult.expression, Left());
          Expression expr2 = AppExpression.make(argResult.expression, Right());
          return ((CheckTypeVisitor.DefCallResult) result).applyExpressions(Arrays.asList(expr1, expr2, argResult.expression));
        }
      }

      result = fixImplicitArgs(result, result.getImplicitParameters(), fun, false);
      if (!result.getParameter().isExplicit()) {
        result = fixImplicitArgs(result, result.getImplicitParameters(), fun, false);
      }
    }

    DependentLink param = result.getParameter();
    if (arg instanceof Concrete.HoleExpression && param.hasNext()) {
      return fixImplicitArgs(result, Collections.singletonList(param), fun, false);
    }

    CheckTypeVisitor.Result argResult = myVisitor.checkArgument(arg, param.hasNext() ? param.getTypeExpr() : null, result);
    if (argResult == null) {
      return null;
    }

    if (!param.hasNext()) {
      CheckTypeVisitor.Result result1 = result.toResult(myVisitor);
      if (!result1.type.isError()) {
        myVisitor.getErrorReporter().report(new NotPiType(argResult.expression, result1.type, fun));
      }
      return null;
    }

    if (param.isExplicit() != isExplicit) {
      reportExplicitnessError(param.isExplicit(), arg);
      return null;
    }

    return result.applyExpression(argResult.expression, myVisitor.getErrorReporter(), fun);
  }

  private void reportExplicitnessError(boolean isExplicit, Concrete.SourceNode sourceNode) {
    myVisitor.getErrorReporter().report(new TypecheckingError("Expected an " + (isExplicit ? "explicit" : "implicit") + " argument", sourceNode));
  }

  private void typecheckDeferredArgument(Pair<InferenceVariable, Concrete.Expression> pair, CheckTypeVisitor.TResult result) {
    CheckTypeVisitor.Result argResult = myVisitor.checkArgument(pair.proj2, pair.proj1.getType(), result);
    Expression argResultExpr = argResult == null ? new ErrorExpression(null, null) : argResult.expression;
    pair.proj1.solve(myVisitor.getEquations(), argResultExpr);
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

    if (result instanceof CheckTypeVisitor.DefCallResult && ((CheckTypeVisitor.DefCallResult) result).getDefinition().getParametersTypecheckingOrder() != null) {
      List<Integer> order = ((CheckTypeVisitor.DefCallResult) result).getDefinition().getParametersTypecheckingOrder();
      List<? extends Expression> resultArgs = ((CheckTypeVisitor.DefCallResult) result).getArguments();
      if (!resultArgs.isEmpty()) {
        order = order.subList(resultArgs.size(), order.size());
      }

      int current = 0; // Position in expr.getArguments()
      int numberOfImplicitArguments = 0; // Number of arguments not present in expr.getArguments()
      Map<Integer,Pair<InferenceVariable,Concrete.Expression>> deferredArguments = new LinkedHashMap<>();
      for (Integer i : order) {
        // Defer arguments up to i
        while (current < expr.getArguments().size()) {
          DependentLink parameter = result.getParameter();
          if (!parameter.hasNext()) {
            break;
          }
          if (!parameter.isExplicit() && expr.getArguments().get(current).isExplicit()) {
            List<? extends DependentLink> implicitParameters = result.getImplicitParameters();
            result = fixImplicitArgs(result, implicitParameters, fun, false);
            parameter = result.getParameter();
            numberOfImplicitArguments += implicitParameters.size();
          }
          if (current + numberOfImplicitArguments >= i) {
            break;
          }

          Concrete.Argument argument = expr.getArguments().get(current);
          if (parameter.isExplicit() != argument.isExplicit()) {
            reportExplicitnessError(parameter.isExplicit(), argument.getExpression());
            return null;
          }
          InferenceVariable var = new ExpressionInferenceVariable(parameter.getTypeExpr(), argument.getExpression(), myVisitor.getAllBindings());
          deferredArguments.put(current + numberOfImplicitArguments, new Pair<>(var, argument.getExpression()));
          result = result.applyExpression(new InferenceReferenceExpression(var, myVisitor.getEquations()), myVisitor.getErrorReporter(), fun);
          current++;
        }

        if (i == current + numberOfImplicitArguments) {
          // If we are at i-th argument, simply typecheck it
          if (current >= expr.getArguments().size()) {
            break;
          }
          Concrete.Argument argument = expr.getArguments().get(current);
          result = inferArg(result, argument.expression, argument.isExplicit(), fun);
          if (result == null) {
            return null;
          }
          current++;
        } else {
          // If i-th argument were deferred, get it from the map and typecheck
          Pair<InferenceVariable, Concrete.Expression> pair = deferredArguments.remove(i);
          if (pair != null) {
            typecheckDeferredArgument(pair, result);
          }
        }
      }

      // Typecheck all deferred arguments
      for (Pair<InferenceVariable, Concrete.Expression> pair : deferredArguments.values()) {
        typecheckDeferredArgument(pair, result);
      }

      // Typecheck the rest of the arguments
      for (; current < expr.getArguments().size(); current++) {
        result = inferArg(result, expr.getArguments().get(current).expression, expr.getArguments().get(current).isExplicit(), fun);
      }
    } else {
      for (Concrete.Argument argument : expr.getArguments()) {
        result = inferArg(result, argument.expression, argument.isExplicit(), fun);
      }
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
        CheckTypeVisitor.Result result1 = result.toResult(myVisitor);
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
