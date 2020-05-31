package org.arend.typechecking.implicitargs;

import org.arend.core.context.binding.inference.ExpressionInferenceVariable;
import org.arend.core.context.binding.inference.FunctionInferenceVariable;
import org.arend.core.context.binding.inference.InferenceVariable;
import org.arend.core.context.binding.inference.TypeClassInferenceVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.context.param.TypedSingleDependentLink;
import org.arend.core.definition.*;
import org.arend.core.expr.*;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.ArgumentExplicitnessError;
import org.arend.ext.error.TypeMismatchError;
import org.arend.naming.reference.TCClassReferable;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.NotPiType;
import org.arend.typechecking.error.local.inference.ArgInferenceError;
import org.arend.typechecking.error.local.inference.InstanceInferenceError;
import org.arend.typechecking.instance.pool.InstancePool;
import org.arend.typechecking.instance.pool.RecursiveInstanceHoleExpression;
import org.arend.typechecking.result.DefCallResult;
import org.arend.typechecking.result.TResult;
import org.arend.typechecking.result.TypecheckingResult;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.arend.util.Pair;

import java.util.*;

import static org.arend.core.expr.ExpressionFactory.*;
import static org.arend.ext.prettyprinting.doc.DocFactory.refDoc;

public class StdImplicitArgsInference implements ImplicitArgsInference {
  private final CheckTypeVisitor myVisitor;

  public StdImplicitArgsInference(CheckTypeVisitor visitor) {
    myVisitor = visitor;
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

    ClassCallExpression type = link.getTypeExpr().cast(ClassCallExpression.class);
    return type != null ? type.getDefinition() : null;
  }

  private TResult fixImplicitArgs(TResult result, List<? extends DependentLink> implicitParameters, Concrete.Expression expr, boolean classVarsOnly, RecursiveInstanceHoleExpression holeExpr) {
    ExprSubstitution substitution = new ExprSubstitution();
    int i = result instanceof DefCallResult ? ((DefCallResult) result).getArguments().size() : 0;
    for (DependentLink parameter : implicitParameters) {
      Expression type = parameter.getTypeExpr().subst(substitution, LevelSubstitution.EMPTY);
      InferenceVariable infVar = null;

      // If result is defCall, then try to infer class instances.
      if (result instanceof DefCallResult) {
        DefCallResult defCallResult = (DefCallResult) result;
        ClassDefinition classDef = getClassRefFromDefCall(defCallResult.getDefinition(), i);
        if (classDef != null && !classDef.isRecord()) {
          Definition.TypeClassParameterKind kind = defCallResult.getDefinition().getTypeClassParameterKind(i);
          if (kind != Definition.TypeClassParameterKind.NO) {
            // If the class does not have a classifying field, infer instance immediately
            if (classDef.getClassifyingField() == null) {
              InstancePool instancePool = kind == Definition.TypeClassParameterKind.ONLY_LOCAL ? myVisitor.getInstancePool().getLocalInstancePool() : myVisitor.getInstancePool();
              TypecheckingResult instanceResult = instancePool.getInstance(null, defCallResult.getParameter().getTypeExpr(), classDef, expr, holeExpr);
              Expression instance;
              if (instanceResult == null) {
                ArgInferenceError error = new InstanceInferenceError(classDef.getReferable(), expr, holeExpr, new Expression[0]);
                myVisitor.getErrorReporter().report(error);
                instance = new ErrorExpression(error);
              } else {
                instance = instanceResult.expression;
              }
              result = result.applyExpression(instance, defCallResult.getParameter().isExplicit(), myVisitor.getErrorReporter(), expr);
              substitution.add(parameter, instance);
              i++;
              continue;
            }

            // Otherwise, generate type class inference variable
            infVar = new TypeClassInferenceVariable(parameter.getName(), type, classDef, kind == Definition.TypeClassParameterKind.ONLY_LOCAL, defCallResult.getDefCall(), holeExpr, myVisitor.getAllBindings());
          }
        }
      }

      // Generate ordinary inference variable
      if (infVar == null) {
        if (classVarsOnly) {
          return result;
        }
        infVar = result instanceof DefCallResult
          ? new FunctionInferenceVariable(((DefCallResult) result).getDefinition(), parameter, i + 1, type, expr, myVisitor.getAllBindings())
          : new FunctionInferenceVariable(null, parameter, i + 1, type, expr, myVisitor.getAllBindings());
      }

      Expression binding = new InferenceReferenceExpression(infVar, myVisitor.getEquations());
      result = result.applyExpression(binding, parameter.isExplicit(), myVisitor.getErrorReporter(), expr);
      substitution.add(parameter, binding);
      i++;
    }
    return result;
  }

  private TResult inferArg(TResult result, Concrete.Expression arg, boolean isExplicit, Concrete.Expression fun) {
    if (result == null) {
      myVisitor.checkExpr(arg, null);
      return null;
    }
    if (arg == null || result instanceof TypecheckingResult && ((TypecheckingResult) result).expression.isError()) {
      myVisitor.checkArgument(arg, null, result);
      return result;
    }

    if (isExplicit) {
      if (result instanceof DefCallResult) {
        DefCallResult defCallResult = (DefCallResult) result;
        if (defCallResult.getDefinition() == Prelude.PATH_CON && defCallResult.getArguments().isEmpty()) {
          SingleDependentLink lamParam = new TypedSingleDependentLink(true, "i", Interval());
          UniverseExpression type = new UniverseExpression(new Sort(defCallResult.getSortArgument().getPLevel(), defCallResult.getSortArgument().getHLevel().add(1)));
          Expression binding = new InferenceReferenceExpression(new FunctionInferenceVariable(Prelude.PATH_CON, Prelude.PATH_CON.getDataTypeParameters(), 1, type, fun, myVisitor.getAllBindings()), myVisitor.getEquations());
          Sort sort = type.getSort().succ();
          result = result.applyExpression(new LamExpression(sort, lamParam, binding), true, myVisitor.getErrorReporter(), fun);

          TypecheckingResult argResult = myVisitor.checkArgument(arg, new PiExpression(sort, lamParam, binding), result);
          if (argResult == null) {
            return null;
          }

          Expression expr1 = AppExpression.make(argResult.expression, Left(), true);
          Expression expr2 = AppExpression.make(argResult.expression, Right(), true);
          return ((DefCallResult) result).applyExpressions(Arrays.asList(expr1, expr2, argResult.expression));
        }
      }

      result = fixImplicitArgs(result, result.getImplicitParameters(), fun, false, null);
    }

    DependentLink param = result.getParameter();
    if (arg instanceof Concrete.HoleExpression && param.hasNext()) {
      return fixImplicitArgs(result, Collections.singletonList(param), fun, false, arg instanceof RecursiveInstanceHoleExpression ? (RecursiveInstanceHoleExpression) arg : null);
    }

    TypecheckingResult argResult = myVisitor.checkArgument(arg, param.hasNext() ? param.getTypeExpr() : null, result);
    if (argResult == null) {
      return null;
    }

    if (!param.hasNext()) {
      TypecheckingResult result1 = result.toResult(myVisitor);
      if (!result1.type.isError()) {
        myVisitor.getErrorReporter().report(new NotPiType(argResult.expression, result1.type, fun));
      }
      return null;
    }

    if (param.isExplicit() != isExplicit) {
      myVisitor.getErrorReporter().report(new ArgumentExplicitnessError(param.isExplicit(), arg));
      return null;
    }

    if (result instanceof DefCallResult && !isExplicit && ((DefCallResult) result).getDefinition() instanceof ClassField) {
      DefCallResult defCallResult = (DefCallResult) result;
      ClassField field = (ClassField) defCallResult.getDefinition();
      ClassCallExpression classCall = argResult.type.normalize(NormalizationMode.WHNF).cast(ClassCallExpression.class);
      PiExpression piType = null;
      if (classCall != null) {
        piType = classCall.getDefinition().getOverriddenType(field, defCallResult.getSortArgument());
      }
      if (piType == null) {
        piType = field.getType(defCallResult.getSortArgument());
      }
      return new TypecheckingResult(FieldCallExpression.make(field, defCallResult.getSortArgument(), argResult.expression), piType.applyExpression(argResult.expression));
    }

    return result.applyExpression(argResult.expression, isExplicit, myVisitor.getErrorReporter(), fun);
  }

  private void typecheckDeferredArgument(Pair<InferenceVariable, Concrete.Expression> pair, TResult result) {
    TypecheckingResult argResult = myVisitor.checkArgument(pair.proj2, pair.proj1.getType(), result);
    Expression argResultExpr = argResult == null ? new ErrorExpression() : argResult.expression;
    pair.proj1.solve(myVisitor.getEquations(), argResultExpr);
  }

  @Override
  public TResult infer(Concrete.AppExpression expr, Expression expectedType) {
    TResult result;
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

    if (result instanceof DefCallResult && expr.getArguments().get(0).isExplicit() && expectedType != null) {
      DefCallResult defCallResult = (DefCallResult) result;
      if (defCallResult.getDefinition() instanceof Constructor && defCallResult.getArguments().size() < DependentLink.Helper.size(((Constructor) defCallResult.getDefinition()).getDataTypeParameters())) {
        DataCallExpression dataCall = expectedType.normalize(NormalizationMode.WHNF).cast(DataCallExpression.class);
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
            boolean ok = true;
            if (dataCall.getUniverseKind() != UniverseKind.NO_UNIVERSES && !Sort.compare(defCallResult.getSortArgument(), dataCall.getSortArgument(), dataCall.getUniverseKind() == UniverseKind.ONLY_COVARIANT ? CMP.LE : CMP.EQ, myVisitor.getEquations(), fun)) {
              ok = false;
            }

            if (ok && !defCallResult.getArguments().isEmpty()) {
              ok = new CompareVisitor(myVisitor.getEquations(), CMP.LE, fun).compareLists(defCallResult.getArguments(), dataCall.getDefCallArguments().subList(0, defCallResult.getArguments().size()), dataCall.getDefinition().getParameters(), dataCall.getDefinition(), new ExprSubstitution());
            }

            if (!ok) {
              myVisitor.getErrorReporter().report(new TypeMismatchError(dataCall, new DataCallExpression(dataCall.getDefinition(), defCallResult.getSortArgument(), args1), fun));
              return null;
            }

            result = DefCallResult.makeTResult(defCallResult.getDefCall(), defCallResult.getDefinition(), dataCall.getSortArgument());
            if (!args1.isEmpty()) {
              result = ((DefCallResult) result).applyExpressions(args1);
            }
          }
        }
      }
    }

    List<Integer> order = result instanceof DefCallResult ? ((DefCallResult) result).getDefinition().getParametersTypecheckingOrder() : null;
    if (order != null) {
      int skip = ((DefCallResult) result).getArguments().size();
      if (skip > 0) {
        List<Integer> newOrder = new ArrayList<>();
        for (Integer index : order) {
          if (index >= skip) {
            newOrder.add(index - skip);
          }
        }

        boolean trivial = true;
        for (int i = 0; i < newOrder.size(); i++) {
          if (i != newOrder.get(i)) {
            trivial = false;
            break;
          }
        }

        order = trivial ? null : newOrder;
      }
    }

    if (order != null) {
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
            result = fixImplicitArgs(result, implicitParameters, fun, false, null);
            parameter = result.getParameter();
            numberOfImplicitArguments += implicitParameters.size();
          }
          if (current + numberOfImplicitArguments >= i) {
            break;
          }

          Concrete.Argument argument = expr.getArguments().get(current);
          if (parameter.isExplicit() != argument.isExplicit()) {
            myVisitor.getErrorReporter().report(new ArgumentExplicitnessError(parameter.isExplicit(), argument.getExpression()));
            return null;
          }
          InferenceVariable var = new ExpressionInferenceVariable(parameter.getTypeExpr(), argument.getExpression(), myVisitor.getAllBindings());
          deferredArguments.put(current + numberOfImplicitArguments, new Pair<>(var, argument.getExpression()));
          result = result.applyExpression(new InferenceReferenceExpression(var, myVisitor.getEquations()), parameter.isExplicit(), myVisitor.getErrorReporter(), fun);
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
  public TResult inferTail(TResult result, Expression expectedType, Concrete.Expression expr) {
    List<? extends DependentLink> actualParams = result.getImplicitParameters();
    int expectedParamsNumber = 0;
    if (expectedType != null) {
      List<SingleDependentLink> expectedParams = new ArrayList<>(actualParams.size());
      expectedType.getPiParameters(expectedParams, true);
      if (expectedParams.size() > actualParams.size()) {
        TypecheckingResult result1 = result.toResult(myVisitor);
        if (!result1.type.isError()) {
          myVisitor.getErrorReporter().report(new TypeMismatchError(expectedType, result1.type, expr));
        }
        return null;
      }
      expectedParamsNumber = expectedParams.size();
    }

    if (expectedParamsNumber != actualParams.size()) {
      result = fixImplicitArgs(result, actualParams.subList(0, actualParams.size() - expectedParamsNumber), expr, expectedType == null || expectedType instanceof Type && ((Type) expectedType).isOmega(), null);
    }

    return result;
  }

  @Override
  public InferenceVariable newInferenceVariable(Expression expectedType, Concrete.SourceNode sourceNode) {
    return new ExpressionInferenceVariable(expectedType, sourceNode, myVisitor.getAllBindings());
  }
}
