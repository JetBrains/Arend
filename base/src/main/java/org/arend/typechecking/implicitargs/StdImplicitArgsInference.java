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
import org.arend.core.expr.visitor.FreeVariablesCollector;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.ArgumentExplicitnessError;
import org.arend.ext.error.TypeMismatchError;
import org.arend.ext.instance.SubclassSearchParameters;
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
import org.arend.util.SingletonList;

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
              TypecheckingResult instanceResult = instancePool.getInstance(null, defCallResult.getParameter().getTypeExpr(), new SubclassSearchParameters(classDef), expr, holeExpr);
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
          Expression binding = new InferenceReferenceExpression(new FunctionInferenceVariable(Prelude.PATH_CON, Prelude.PATH_CON.getDataTypeParameters(), 1, new UniverseExpression(defCallResult.getSortArgument()), fun, myVisitor.getAllBindings()), myVisitor.getEquations());
          Sort sort = defCallResult.getSortArgument().succ();
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
    if (!param.hasNext() && result instanceof TypecheckingResult) {
      TypecheckingResult coercedResult = CoerceData.coerceToKey(((TypecheckingResult) result), new CoerceData.PiKey(), fun, myVisitor);
      if (coercedResult != null) {
        result = coercedResult;
        param = result.getParameter();
      }
    }
    if (arg instanceof Concrete.HoleExpression && param.hasNext()) {
      if (!isExplicit && param.isExplicit()) {
        myVisitor.getErrorReporter().report(new ArgumentExplicitnessError(true, arg));
      }
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

    if (result instanceof DefCallResult && ((DefCallResult) result).getDefinition() == Prelude.SUC) {
      Expression type = argResult.type.normalize(NormalizationMode.WHNF);
      if (type instanceof DataCallExpression && ((DataCallExpression) type).getDefinition() == Prelude.FIN) {
        return new TypecheckingResult(Suc(argResult.expression), new DataCallExpression(Prelude.FIN, ((DataCallExpression) type).getSortArgument(), new SingletonList<>(Suc(((DataCallExpression) type).getDefCallArguments().get(0)))));
      }
    }

    return result.applyExpression(argResult.expression, isExplicit, myVisitor.getErrorReporter(), fun);
  }

  private void typecheckDeferredArgument(Pair<InferenceVariable, Concrete.Expression> pair, TResult result) {
    TypecheckingResult argResult = myVisitor.checkArgument(pair.proj2, pair.proj1.getType(), result);
    Expression argResultExpr = argResult == null ? new ErrorExpression() : argResult.expression;
    pair.proj1.solve(myVisitor, argResultExpr);
  }

  @Override
  public TResult infer(Concrete.AppExpression expr, Expression expectedType) {
    TResult result;
    Concrete.Expression fun = expr.getFunction();
    if (fun instanceof Concrete.ReferenceExpression) {
      Concrete.ReferenceExpression refExpr = (Concrete.ReferenceExpression) fun;
      if (!expr.getArguments().get(0).isExplicit() && (refExpr.getReferent() == Prelude.ZERO.getRef() || refExpr.getReferent() == Prelude.SUC.getRef())) {
        TypecheckingResult argResult = myVisitor.checkExpr(expr.getArguments().get(0).getExpression(), Nat());
        if (argResult == null) {
          return null;
        }

        if (refExpr.getReferent() == Prelude.ZERO.getRef()) {
          result = new TypecheckingResult(new SmallIntegerExpression(0), Fin(Suc(argResult.expression)));
          if (expr.getArguments().size() > 1) {
            myVisitor.getErrorReporter().report(new NotPiType(argResult.expression, result.getType(), fun));
            return null;
          }
          return result;
        }

        if (expr.getArguments().size() == 1) {
          SingleDependentLink param = new TypedSingleDependentLink(true, "x", Fin(argResult.expression));
          return new TypecheckingResult(new LamExpression(Sort.SET0, param, Suc(new ReferenceExpression(param))), new PiExpression(Sort.SET0, param, Fin(Suc(argResult.expression))));
        }

        TypecheckingResult arg2Result = myVisitor.checkExpr(expr.getArguments().get(1).getExpression(), Fin(argResult.expression));
        if (arg2Result == null) {
          return null;
        }

        result = new TypecheckingResult(Suc(arg2Result.expression), Fin(Suc(argResult.expression)));
        if (expr.getArguments().size() > 2) {
          myVisitor.getErrorReporter().report(new NotPiType(arg2Result.expression, result.getType(), fun));
          return null;
        }
        return result;
      }

      result = myVisitor.visitReference(refExpr);
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

    Definition definition = result instanceof DefCallResult ? ((DefCallResult) result).getDefinition() : null;
    List<Integer> order = definition != null ? definition.getParametersTypecheckingOrder() : null;
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
        if (i == -1) {
          Expression expectedType1 = dropPiParameters(definition, expr.getArguments(), expectedType);
          if (expectedType1 != null) {
            new CompareVisitor(myVisitor.getEquations(), CMP.LE, expr).compare(result.getType(), expectedType1, Type.OMEGA, false);
          }
          continue;
        }

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
      int i = 0;
      if (expectedType != null && expectedType.getStuckInferenceVariable() == null) {
        for (; i < expr.getArguments().size(); i++) {
          Concrete.Argument argument = expr.getArguments().get(i);
          if (result instanceof TypecheckingResult && argument.isExplicit()) {
            DependentLink param = result.getParameter();
            if (param.hasNext() && !param.isExplicit()) {
              break;
            }
          }
          result = inferArg(result, argument.expression, argument.isExplicit(), fun);
        }

        if (result == null || i == expr.getArguments().size()) {
          return result;
        }

        Pair<Expression, Integer> pair = normalizePi(expr.getArguments(), i, result.getType());
        ((TypecheckingResult) result).type = pair.proj1;

        for (; i < pair.proj2; i++) {
          Concrete.Argument argument = expr.getArguments().get(i);
          result = inferArg(result, argument.expression, argument.isExplicit(), fun);
        }

        if (result == null) {
          return null;
        }
        if (i < expr.getArguments().size()) {
          result = fixImplicitArgs(result, result.getImplicitParameters(), fun, false, null);
          Expression actualType = dropPiParameters(result.getType(), expr.getArguments(), i);
          if (actualType != null) {
            new CompareVisitor(myVisitor.getEquations(), CMP.LE, fun).compare(actualType, expectedType, Type.OMEGA, false);
          }
        }
      }

      for (; i < expr.getArguments().size(); i++) {
        Concrete.Argument argument = expr.getArguments().get(i);
        result = inferArg(result, argument.expression, argument.isExplicit(), fun);
      }
    }

    return result;
  }

  /** Normalizes {@param type}
   * @return (normalized type, actual type that will be compared with expected type, the first index in arguments from which type is non-dependent pi-type)
  */
  private Pair<Expression, Integer> normalizePi(List<? extends ConcreteArgument> arguments, int startIndex, Expression type) {
    List<PiExpression> piTypes = new ArrayList<>();
    int i = startIndex;
    boolean ok = true;
    loop:
    while (true) {
      type = type.normalize(NormalizationMode.WHNF);
      if (!(type instanceof PiExpression)) {
        break;
      }

      PiExpression pi = (PiExpression) type;
      piTypes.add(pi);
      type = pi.getCodomain();
      SingleDependentLink link = pi.getParameters();
      for (; i < arguments.size() && link.hasNext(); link = link.getNext()) {
        if (!arguments.get(i).isExplicit() && link.isExplicit()) {
          ok = false;
          break loop;
        }
        if (arguments.get(i).isExplicit() == link.isExplicit()) {
          i++;
        }
      }
      if (i == arguments.size()) {
        if (link.hasNext()) {
          ok = link.isExplicit();
        } else {
          type = type.normalize(NormalizationMode.WHNF);
          if (type instanceof PiExpression) {
            ok = ((PiExpression) type).getParameters().isExplicit();
          } else if (type instanceof ReferenceExpression) {
            loop2:
            for (PiExpression pi2 : piTypes) {
              for (SingleDependentLink link2 = pi2.getParameters(); link2.hasNext(); link2 = link2.getNext()) {
                if (((ReferenceExpression) type).getBinding() == link2) {
                  ok = false;
                  break loop2;
                }
              }
            }
          }
        }
        break;
      }
    }

    Expression result = type;
    for (int j = piTypes.size() - 1; j >= 0; j--) {
      result = new PiExpression(piTypes.get(j).getResultSort(), piTypes.get(j).getParameters(), result);
    }

    if (!ok) {
      return new Pair<>(result, arguments.size());
    }

    // find the last pi-parameter that occurs in the actual type
    FreeVariablesCollector collector = new FreeVariablesCollector();
    type.accept(collector, null);
    SingleDependentLink param = null;
    for (int j = piTypes.size() - 1; j >= 0; j--) {
      for (SingleDependentLink link = piTypes.get(j).getParameters(); link.hasNext(); link = link.getNext()) {
        if (collector.getResult().contains(link)) {
          param = link;
        }
      }
      if (param != null) {
        if (param.getNext().hasNext()) {
          param = param.getNext();
        } else if (j < piTypes.size() - 1) {
          param = piTypes.get(j + 1).getParameters();
        } else {
          // all parameters occur in the result type
          return new Pair<>(result, arguments.size());
        }
        break;
      }
    }
    if (param == null) {
      param = piTypes.get(0).getParameters();
    }

    i = startIndex;
    for (PiExpression pi : piTypes) {
      for (SingleDependentLink link = pi.getParameters(); i < arguments.size() && link.hasNext(); link = link.getNext()) {
        if (link == param) {
          return new Pair<>(result, i);
        }
        if (arguments.get(i).isExplicit() == link.isExplicit()) {
          i++;
        }
      }
    }

    return new Pair<>(result, arguments.size());
  }

  private Expression dropPiParameters(Expression type, List<? extends ConcreteArgument> arguments, int i) {
    while (i < arguments.size()) {
      if (!(type instanceof PiExpression)) {
        return null;
      }
      PiExpression pi = (PiExpression) type;
      type = pi.getCodomain();
      SingleDependentLink param = pi.getParameters();
      for (; param.hasNext() && i < arguments.size(); param = param.getNext(), i++) {
        while (param.hasNext() && param.isExplicit() != arguments.get(i).isExplicit()) {
          param = param.getNext();
        }
      }
      if (i == arguments.size()) {
        return param.hasNext() ? new PiExpression(pi.getResultSort(), param, type) : type;
      }
    }
    return type;
  }

  private Expression dropPiParameters(Definition definition, List<? extends ConcreteArgument> arguments, Expression expectedType) {
    if (expectedType == null) {
      return null;
    }

    DependentLink param = definition.getParameters();
    for (ConcreteArgument argument : arguments) {
      while (param.hasNext() && !param.isExplicit() && argument.isExplicit()) {
        param = param.getNext();
      }
      if (!param.hasNext()) {
        return null;
      }
      if (argument.isExplicit() == param.isExplicit()) {
        param = param.getNext();
      }
    }

    while (param.hasNext()) {
      PiExpression piExpr = expectedType.normalize(NormalizationMode.WHNF).cast(PiExpression.class);
      if (piExpr == null) {
        return null;
      }

      SingleDependentLink piParam = piExpr.getParameters();
      for (; piParam.hasNext() && param.hasNext(); piParam = piParam.getNext(), param = param.getNext()) {
        if (param.isExplicit() != piParam.isExplicit()) {
          return null;
        }
      }

      expectedType = piParam.hasNext() ? new PiExpression(piExpr.getResultSort(), piParam, piExpr.getCodomain()) : piExpr.getCodomain();
    }

    return expectedType;
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
