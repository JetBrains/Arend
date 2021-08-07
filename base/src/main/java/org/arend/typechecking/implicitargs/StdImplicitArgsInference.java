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
import org.arend.core.expr.type.TypeExpression;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.core.expr.visitor.FreeVariablesCollector;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.Levels;
import org.arend.ext.core.level.LevelSubstitution;
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
import org.arend.ext.util.Pair;
import org.arend.util.SingletonList;
import org.jetbrains.annotations.NotNull;

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
              TypecheckingResult instanceResult;
              if (expr instanceof Concrete.LongReferenceExpression && i == 0 && ((Concrete.LongReferenceExpression) expr).getQualifier() != null) {
                instanceResult = myVisitor.checkExpr(Objects.requireNonNull(((Concrete.LongReferenceExpression) expr).getQualifier()), type);
              } else {
                instanceResult = instancePool.findInstance(null, defCallResult.getParameter().getTypeExpr(), new SubclassSearchParameters(classDef), expr, holeExpr, myVisitor.getDefinition());
              }
              Expression instance;
              if (instanceResult == null) {
                ArgInferenceError error = new InstanceInferenceError(classDef.getReferable(), expr, holeExpr, new Expression[0]);
                myVisitor.getErrorReporter().report(error);
                instance = new ErrorExpression(error);
              } else {
                instance = instanceResult.expression;
              }
              result = result.applyExpression(instance, defCallResult.getParameter().isExplicit(), myVisitor, expr);
              substitution.add(parameter, instance);
              i++;
              continue;
            }

            // Otherwise, generate type class inference variable
            infVar = new TypeClassInferenceVariable(parameter.getName(), type, classDef, defCallResult.getDefinition() instanceof ClassField, kind == Definition.TypeClassParameterKind.ONLY_LOCAL, defCallResult.getDefCall(), holeExpr, myVisitor.getDefinition(), myVisitor.getAllBindings());
          }
        }
      }

      // Generate ordinary inference variable
      if (infVar == null) {
        if (classVarsOnly) {
          return result;
        }
        Definition definition;
        if (result instanceof DefCallResult) {
          definition = ((DefCallResult) result).getDefinition();
        } else if (result instanceof TypecheckingResult && ((TypecheckingResult) result).getExpression() instanceof DefCallExpression) {
          definition = ((DefCallExpression) ((TypecheckingResult) result).getExpression()).getDefinition();
        } else {
          definition = null;
        }
        infVar = new FunctionInferenceVariable(definition, parameter, i + 1, type, expr, myVisitor.getAllBindings());
      }

      Expression argument;
      if (expr instanceof Concrete.LongReferenceExpression &&
              ((Concrete.LongReferenceExpression) expr).getQualifier() != null &&
              i == 0 && result instanceof DefCallResult &&
              ((DefCallResult) result).getDefinition() instanceof ClassField) {
        argument = myVisitor.checkExpr(Objects.requireNonNull(((Concrete.LongReferenceExpression) expr).getQualifier()), type).expression;
      } else {
        argument = InferenceReferenceExpression.make(infVar, myVisitor.getEquations());
      }
      result = result.applyExpression(argument, parameter.isExplicit(), myVisitor, expr);
      substitution.add(parameter, argument);
      i++;
    }
    return result;
  }

  private TResult inferArg(TResult result, Concrete.Expression arg, boolean isExplicit, Concrete.Expression fun) {
    if (result == null) {
      myVisitor.checkExpr(arg, null);
      return null;
    }
    if (arg == null || result instanceof TypecheckingResult && ((TypecheckingResult) result).expression.reportIfError(myVisitor.getErrorReporter(), fun)) {
      myVisitor.checkArgument(arg, null, result, null);
      return result;
    }

    if (isExplicit) {
      if (result instanceof DefCallResult && ((DefCallResult) result).getDefinition() == Prelude.PATH_CON) {
        DefCallResult defCallResult = (DefCallResult) result;
        SingleDependentLink lamParam = new TypedSingleDependentLink(true, "i", Interval());
        Sort sort0 = Sort.STD.subst(defCallResult.getLevels().toLevelPair());
        Sort sort = sort0.succ();
        TypecheckingResult argResult;
        if (defCallResult.getArguments().isEmpty()) {
          Expression binding = InferenceReferenceExpression.make(new FunctionInferenceVariable(Prelude.PATH_CON, Prelude.PATH_CON.getDataTypeParameters(), 1, new UniverseExpression(sort0), fun, myVisitor.getAllBindings()), myVisitor.getEquations());
          result = result.applyExpression(new LamExpression(sort, lamParam, binding), true, myVisitor, fun);
          argResult = myVisitor.checkArgument(arg, new PiExpression(sort, lamParam, binding), result, null);
        } else {
          argResult = myVisitor.checkArgument(arg, new PiExpression(sort, lamParam, AppExpression.make(defCallResult.getArguments().get(0), new ReferenceExpression(lamParam), true)), result, null);
        }
        return argResult == null ? null : ((DefCallResult) result).applyPathArgument(argResult.expression, myVisitor, arg);
      }

      result = fixImplicitArgs(result, result.getImplicitParameters(), fun, false, null);
    }

    DependentLink param = result.getParameter();
    if (!param.hasNext() && result instanceof TypecheckingResult) {
      TResult coercedResult = CheckTypeVisitor.coerceFromType((TypecheckingResult) result);
      if (coercedResult != null) {
        result = coercedResult;
        if (isExplicit) {
          result = fixImplicitArgs(result, result.getImplicitParameters(), fun, false, null);
        }
        param = result.getParameter();
      }
      if (!param.hasNext()) {
        TypecheckingResult tcResult = ((TypecheckingResult) result).normalizeType();
        result = tcResult;
        if (tcResult.type instanceof DataCallExpression && ((DataCallExpression) tcResult.type).getDefinition() == Prelude.PATH) {
          List<Expression> args = ((DataCallExpression) tcResult.type).getDefCallArguments();
          result = DefCallResult.makeTResult(new Concrete.ReferenceExpression(fun.getData(), Prelude.AT.getRef()), Prelude.AT, ((DataCallExpression) tcResult.type).getLevels())
            .applyExpression(args.get(0), false, myVisitor, fun)
            .applyExpression(args.get(1), false, myVisitor, fun)
            .applyExpression(args.get(2), false, myVisitor, fun)
            .applyExpression(tcResult.expression, true, myVisitor, fun);
          param = result.getParameter();
        } else {
          coercedResult = CoerceData.coerceToKey(tcResult, new CoerceData.PiKey(), fun, myVisitor);
          if (coercedResult != null) {
            result = coercedResult;
            if (isExplicit) {
              result = fixImplicitArgs(result, result.getImplicitParameters(), fun, false, null);
            }
            param = result.getParameter();
          }
        }
      }
    }
    if (arg instanceof Concrete.HoleExpression && param.hasNext()) {
      if (!isExplicit && param.isExplicit()) {
        myVisitor.getErrorReporter().report(new ArgumentExplicitnessError(true, arg));
      }
      return fixImplicitArgs(result, Collections.singletonList(param), fun, false, arg instanceof RecursiveInstanceHoleExpression ? (RecursiveInstanceHoleExpression) arg : null);
    }

    TypecheckingResult argResult = myVisitor.checkArgument(arg, param.hasNext() ? param.getTypeExpr() : null, result, null);
    if (argResult == null) {
      return null;
    }

    if (!param.hasNext()) {
      TypecheckingResult result1 = result.toResult(myVisitor);
      if (!result1.type.reportIfError(myVisitor.getErrorReporter(), fun)) {
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
        piType = classCall.getDefinition().getOverriddenType(field, defCallResult.getLevels());
      }
      if (piType == null) {
        piType = field.getType(defCallResult.getLevels());
      }
      return new TypecheckingResult(FieldCallExpression.make(field, argResult.expression), piType.applyExpression(argResult.expression));
    }

    if (result instanceof DefCallResult && ((DefCallResult) result).getDefinition() == Prelude.SUC) {
      Expression type = argResult.type.normalize(NormalizationMode.WHNF);
      if (type instanceof DataCallExpression && ((DataCallExpression) type).getDefinition() == Prelude.FIN) {
        return new TypecheckingResult(Suc(argResult.expression), new DataCallExpression(Prelude.FIN, ((DataCallExpression) type).getLevels(), new SingletonList<>(Suc(((DataCallExpression) type).getDefCallArguments().get(0)))));
      }
    }

    return result.applyExpression(argResult.expression, isExplicit, myVisitor, fun);
  }

  private void typecheckDeferredArgument(Pair<InferenceVariable, Concrete.Expression> pair, TResult result) {
    TypecheckingResult argResult = myVisitor.checkArgument(pair.proj2, pair.proj1.getType(), result, pair.proj1);
    Expression argResultExpr = argResult == null ? new ErrorExpression() : argResult.expression;
    pair.proj1.solve(myVisitor, argResultExpr);
  }

  private InferenceVariable getInferenceVariableFromElementsType(Expression elementsType) {
    if (elementsType == null) return null;
    if (elementsType instanceof LamExpression) {
      elementsType = ((LamExpression) elementsType).getBody().normalize(NormalizationMode.WHNF);
      if (elementsType instanceof AppExpression && ((AppExpression) elementsType).isExplicit()) {
        elementsType = elementsType.getFunction().normalize(NormalizationMode.WHNF);
      }
    }
    return elementsType instanceof InferenceReferenceExpression ? elementsType.getInferenceVariable() : null;
  }

  private TResult checkArrayCons(DefCallResult defCallResult, List<Concrete.Argument> arguments, Expression expectedType, Concrete.Expression fun) {
    int index = 0;
    Expression length = null;
    Definition definition = defCallResult.getDefinition();
    if (definition == Prelude.ARRAY_CONS && !arguments.isEmpty() && !arguments.get(0).isExplicit()) {
      TypecheckingResult result = myVisitor.checkExpr(arguments.get(0).expression, Nat());
      if (result == null) return null;
      length = result.expression;
      index++;
    }

    Sort sort = defCallResult.getLevels().toLevelPair().toSort();
    Sort sort0 = sort.max(Sort.SET0);
    Expression elementsType = null;
    if (index < arguments.size() && !arguments.get(index).isExplicit()) {
      TypecheckingResult result = myVisitor.checkExpr(arguments.get(index).expression, definition == Prelude.EMPTY_ARRAY ? new PiExpression(sort.succ(), new TypedSingleDependentLink(true, null, Fin(Zero())), new UniverseExpression(sort)) : length == null ? null : new PiExpression(sort.succ(), new TypedSingleDependentLink(true, null, Fin(Suc(length))), new UniverseExpression(sort)));
      if (result == null) return null;
      elementsType = result.expression;
      index++;
    }

    for (; index < arguments.size() && !arguments.get(index).isExplicit(); index++) {
      myVisitor.getErrorReporter().report(new ArgumentExplicitnessError(true, arguments.get(index).expression));
    }

    int index2 = index + 1;
    for (; index2 < arguments.size() && !arguments.get(index2).isExplicit(); index2++) {
      myVisitor.getErrorReporter().report(new ArgumentExplicitnessError(true, arguments.get(index2).expression));
    }

    TResult result = defCallResult;
    if (expectedType != null) {
      ClassCallExpression expectedClassCall = TypeConstructorExpression.unfoldType(expectedType).cast(ClassCallExpression.class);
      if (expectedClassCall != null) {
        if (expectedClassCall.getDefinition() != Prelude.DEP_ARRAY || !expectedClassCall.getLevels().compare(defCallResult.getLevels(), CMP.LE, myVisitor.getEquations(), fun)) {
          myVisitor.getErrorReporter().report(new TypeMismatchError(expectedClassCall, refDoc(Prelude.DEP_ARRAY.getRef()), fun));
          return null;
        }
        if (elementsType == null) {
          elementsType = expectedClassCall.getAbsImplementationHere(Prelude.ARRAY_ELEMENTS_TYPE);
        }
        if (definition != Prelude.EMPTY_ARRAY && length == null) {
          length = expectedClassCall.getAbsImplementationHere(Prelude.ARRAY_LENGTH);
          length = length == null ? null : length.normalize(NormalizationMode.WHNF).pred();
        }
      }
    }

    if (definition == Prelude.EMPTY_ARRAY) {
      if (elementsType != null) {
        result = result.applyExpression(elementsType, false, myVisitor, fun);
      }
    } else {
      if (length != null) {
        result = result.applyExpression(length, false, myVisitor, fun);
        if (elementsType != null) {
          result = result.applyExpression(elementsType, false, myVisitor, fun);
        }
      }
    }

    defCallResult = result instanceof DefCallResult ? (DefCallResult) result : null;
    if (definition == Prelude.ARRAY_CONS && defCallResult != null && defCallResult.getArguments().isEmpty() && index2 < arguments.size()) {
      InferenceVariable var = null;
      Expression constType;
      ClassCallExpression argClassCall = null;
      if (elementsType != null) {
        elementsType = elementsType.normalize(NormalizationMode.WHNF);
        var = getInferenceVariableFromElementsType(elementsType);
        if (var == null) {
          constType = elementsType.removeConstLam();
          if (constType != null && constType.getInferenceVariable() != null) {
            constType = null;
          }
          if (constType != null) {
            Map<ClassField, Expression> impls = new HashMap<>();
            argClassCall = new ClassCallExpression(Prelude.DEP_ARRAY, defCallResult.getLevels(), impls, Sort.STD, UniverseKind.NO_UNIVERSES);
            impls.put(Prelude.ARRAY_ELEMENTS_TYPE, new LamExpression(sort0, new TypedSingleDependentLink(true, null, Fin(FieldCallExpression.make(Prelude.ARRAY_LENGTH, new ReferenceExpression(argClassCall.getThisBinding())))), constType));
          }
        }
      }
      if (argClassCall == null) {
        argClassCall = new ClassCallExpression(Prelude.DEP_ARRAY, defCallResult.getLevels());
      }
      TypecheckingResult result2 = myVisitor.checkExpr(arguments.get(index2).expression, argClassCall);
      if (result2 == null) return null;
      ClassCallExpression classCall = result2.type.normalize(NormalizationMode.WHNF).cast(ClassCallExpression.class);
      if (classCall != null && classCall.getDefinition() != Prelude.DEP_ARRAY) {
        myVisitor.getErrorReporter().report(new TypeMismatchError(refDoc(Prelude.DEP_ARRAY.getRef()), result2.type, arguments.get(index2).expression));
        return null;
      }

      TypecheckingResult result1 = null;
      boolean checked = false;
      if (classCall != null && (elementsType == null || var != null)) {
        Expression elementsType1 = classCall.getAbsImplementationHere(Prelude.ARRAY_ELEMENTS_TYPE);
        if (elementsType1 != null) {
          elementsType = elementsType1.normalize(NormalizationMode.WHNF);
          var = getInferenceVariableFromElementsType(elementsType);
        }
      }

      if (var == null) {
        constType = elementsType == null ? null : elementsType.removeConstLam();
        if (constType != null && constType.getInferenceVariable() == null) {
          result1 = myVisitor.checkExpr(arguments.get(index).expression, constType);
          if (result1 == null) return null;
          checked = true;
          if (length == null) length = classCall == null ? null : classCall.getAbsImplementationHere(Prelude.ARRAY_LENGTH);
          if (length == null) length = FieldCallExpression.make(Prelude.ARRAY_LENGTH, result2.expression);
          result = result
            .applyExpression(length, false, myVisitor, fun)
            .applyExpression(new LamExpression(sort0, new TypedSingleDependentLink(true, null, new DataCallExpression(Prelude.FIN, Levels.EMPTY, new SingletonList<>(Suc(length)))), constType), false, myVisitor, fun);
        }
      }

      if (!checked) {
        result1 = myVisitor.checkExpr(arguments.get(index).expression, null);
        if (result1 == null) return null;

        if (var != null) {
          if (length == null) length = classCall == null ? null : classCall.getAbsImplementationHere(Prelude.ARRAY_LENGTH);
          if (length == null) length = FieldCallExpression.make(Prelude.ARRAY_LENGTH, result2.expression);
          Expression actualElementsType = new LamExpression(sort0, new TypedSingleDependentLink(true, null, new DataCallExpression(Prelude.FIN, Levels.EMPTY, new SingletonList<>(length))), result1.type);
          if (new CompareVisitor(myVisitor.getEquations(), CMP.LE, fun).normalizedCompare(actualElementsType, elementsType, null, false)) {
            checked = true;
            result = result
              .applyExpression(length, false, myVisitor, fun)
              .applyExpression(new LamExpression(sort0, new TypedSingleDependentLink(true, null, new DataCallExpression(Prelude.FIN, Levels.EMPTY, new SingletonList<>(Suc(length)))), result1.type), false, myVisitor, fun);
          }
        }

        if (!checked) {
          result = fixImplicitArgs(result, result.getImplicitParameters(), fun, false, null);
          List<? extends Expression> args = ((DefCallResult) result).getArguments();
          Expression expected1 = AppExpression.make(args.get(1), Zero(), true);
          if (!new CompareVisitor(myVisitor.getEquations(), CMP.LE, fun).normalizedCompare(result1.type, expected1, null, false)) {
            myVisitor.getErrorReporter().report(new TypeMismatchError(expected1, result1.type, arguments.get(index).expression));
            return null;
          }
          Map<ClassField, Expression> impls = new LinkedHashMap<>();
          impls.put(Prelude.ARRAY_LENGTH, args.get(0));
          TypedSingleDependentLink lamParam = new TypedSingleDependentLink(true, "j", new DataCallExpression(Prelude.FIN, Levels.EMPTY, new SingletonList<>(args.get(0))));
          impls.put(Prelude.ARRAY_ELEMENTS_TYPE, new LamExpression(sort0, lamParam, AppExpression.make(args.get(1), Suc(new ReferenceExpression(lamParam)), true)));
          Expression expected2 = new ClassCallExpression(Prelude.DEP_ARRAY, defCallResult.getLevels(), impls, Sort.STD, UniverseKind.NO_UNIVERSES);
          if (!new CompareVisitor(myVisitor.getEquations(), CMP.LE, fun).normalizedCompare(result2.type, expected2, null, false)) {
            myVisitor.getErrorReporter().report(new TypeMismatchError(expected2, result2.type, arguments.get(index2).expression));
            return null;
          }
        }
      }

      result = result
        .applyExpression(result1.expression, true, myVisitor, arguments.get(index).expression)
        .applyExpression(result2.expression, true, myVisitor, arguments.get(index2).expression);
      index = index2 + 1;
    } else if (definition == Prelude.ARRAY_CONS && elementsType == null && index2 == arguments.size() && index < arguments.size() && defCallResult != null && defCallResult.getArguments().isEmpty()) {
      TypecheckingResult result1 = myVisitor.checkExpr(arguments.get(index).expression, null);
      if (result1 == null) return null;
      Sort sort1 = result1.type.getSortOfType();
      if (sort1 == null) return null;
      if (!Sort.compare(sort1, sort, CMP.LE, myVisitor.getEquations(), arguments.get(index).expression)) {
        return null;
      }

      Type type;
      if (length == null) {
        type = new TypeExpression(FunCallExpression.make(Prelude.ARRAY, defCallResult.getLevels(), new SingletonList<>(result1.type)), sort);
      } else {
        Map<ClassField, Expression> impls = new LinkedHashMap<>();
        impls.put(Prelude.ARRAY_LENGTH, length);
        impls.put(Prelude.ARRAY_ELEMENTS_TYPE, new LamExpression(sort0, new TypedSingleDependentLink(true, null, Fin(length)), result1.type));
        type = new ClassCallExpression(Prelude.DEP_ARRAY, defCallResult.getLevels(), impls, Sort.STD, UniverseKind.NO_UNIVERSES);
      }

      TypedSingleDependentLink lamParam = new TypedSingleDependentLink(true, "l", type);
      if (length == null) length = FieldCallExpression.make(Prelude.ARRAY_LENGTH, new ReferenceExpression(lamParam));
      elementsType = new LamExpression(sort0, new TypedSingleDependentLink(true, null, Fin(Suc(length))), result1.type);
      Expression resultExpr = new LamExpression(sort, lamParam, ArrayExpression.make(defCallResult.getLevels().toLevelPair(), elementsType, new SingletonList<>(result1.expression), new ReferenceExpression(lamParam)));
      return new TypecheckingResult(resultExpr, resultExpr.getType());
    } else if (index + 1 < index2) {
      arguments.subList(index + 1, index2).clear();
    }

    for (; index < arguments.size(); index++) {
      result = inferArg(result, arguments.get(index).expression, arguments.get(index).isExplicit(), fun);
    }

    return result;
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
    } else if (fun instanceof Concrete.GoalExpression) {
      List<TypecheckingResult> argumentResults = new ArrayList<>();
      for (Concrete.Argument argument : expr.getArguments()) {
        var typecheckedArgument = myVisitor.checkExpr(argument.expression, null);
        if (typecheckedArgument != null) {
          TypecheckingResult normalized = typecheckedArgument.normalizeType();
          argumentResults.add(normalized);
        }
      }
      Expression expectedGoalType = null;
      if (expectedType != null && argumentResults.size() == expr.getArguments().size()) {
          expectedGoalType = generatePiExpressionByArguments(expectedType, argumentResults, expr.getArguments(), fun);
      }
      TypecheckingResult goalCheckingResult = myVisitor.checkExpr(fun, expectedGoalType);
      Expression appExpr;
      if (argumentResults.size() == expr.getArguments().size()) {
        appExpr = generateAppExpressionByArguments(goalCheckingResult.expression, argumentResults, expr.getArguments());
      } else {
        appExpr = goalCheckingResult.expression;
      }
      return new TypecheckingResult(appExpr, expectedType);
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
        DataCallExpression dataCall = TypeConstructorExpression.unfoldType(expectedType).cast(DataCallExpression.class);
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
            boolean ok = dataCall.getLevels().compare(defCallResult.getLevels(), CMP.LE, myVisitor.getEquations(), fun);

            if (ok && !defCallResult.getArguments().isEmpty()) {
              ok = new CompareVisitor(myVisitor.getEquations(), CMP.LE, fun).compareLists(defCallResult.getArguments(), dataCall.getDefCallArguments().subList(0, defCallResult.getArguments().size()), dataCall.getDefinition().getParameters(), dataCall.getDefinition(), new ExprSubstitution());
            }

            if (!ok) {
              myVisitor.getErrorReporter().report(new TypeMismatchError(dataCall, new DataCallExpression(dataCall.getDefinition(), defCallResult.getLevels(), args1), fun));
              return null;
            }

            if (!args1.isEmpty()) {
              result = ((DefCallResult) result).applyExpressions(args1);
            }
          }
        }
      }
    }

    DefCallResult defCallResult = result instanceof DefCallResult ? (DefCallResult) result : null;
    Definition definition = defCallResult != null ? defCallResult.getDefinition() : null;
    List<Concrete.Argument> arguments = expr.getArguments();
    if ((definition == Prelude.EMPTY_ARRAY || definition == Prelude.ARRAY_CONS) && defCallResult != null && defCallResult.getArguments().isEmpty()) {
      return checkArrayCons(defCallResult, arguments, expectedType, fun);
    }

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
      int current = 0; // Position in arguments
      int numberOfImplicitArguments = 0; // Number of arguments not present in arguments
      Map<Integer,Pair<InferenceVariable,Concrete.Expression>> deferredArguments = new LinkedHashMap<>();
      for (Integer i : order) {
        if (i == -1) {
          Expression expectedType1 = dropPiParameters(definition, arguments, expectedType);
          if (expectedType1 != null) {
            new CompareVisitor(myVisitor.getEquations(), CMP.LE, expr).compare(result.getType(), expectedType1, Type.OMEGA, false);
          }
          continue;
        }

        // Defer arguments up to i
        while (current < arguments.size()) {
          DependentLink parameter = result.getParameter();
          if (!parameter.hasNext()) {
            break;
          }
          if (!parameter.isExplicit() && arguments.get(current).isExplicit()) {
            List<? extends DependentLink> implicitParameters = result.getImplicitParameters();
            result = fixImplicitArgs(result, implicitParameters, fun, false, null);
            parameter = result.getParameter();
            numberOfImplicitArguments += implicitParameters.size();
          }
          if (current + numberOfImplicitArguments >= i) {
            break;
          }

          Concrete.Argument argument = arguments.get(current);
          if (parameter.isExplicit() != argument.isExplicit()) {
            myVisitor.getErrorReporter().report(new ArgumentExplicitnessError(parameter.isExplicit(), argument.getExpression()));
            return null;
          }
          InferenceVariable var = new ExpressionInferenceVariable(parameter.getTypeExpr(), argument.getExpression(), myVisitor.getAllBindings(), false);
          deferredArguments.put(current + numberOfImplicitArguments, new Pair<>(var, argument.getExpression()));
          result = result.applyExpression(new InferenceReferenceExpression(var), parameter.isExplicit(), myVisitor, fun);
          current++;
        }

        if (i == current + numberOfImplicitArguments) {
          // If we are at i-th argument, simply typecheck it
          if (current >= arguments.size()) {
            break;
          }
          Concrete.Argument argument = arguments.get(current);
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
      for (; current < arguments.size(); current++) {
        result = inferArg(result, arguments.get(current).expression, arguments.get(current).isExplicit(), fun);
      }
    } else {
      if (result instanceof DefCallResult && ((DefCallResult) result).getDefinition() instanceof ClassField && (arguments.isEmpty() || arguments.get(0).isExplicit())) {
        arguments = new ArrayList<>(expr.getArguments().size() + 1);
        arguments.add(new Concrete.Argument(new Concrete.HoleExpression(fun.getData()), false));
        arguments.addAll(expr.getArguments());
      }

      int i = 0;
      if (expectedType != null && expectedType.getStuckInferenceVariable() == null) {
        for (; i < arguments.size(); i++) {
          Concrete.Argument argument = arguments.get(i);
          if (result instanceof TypecheckingResult && argument.isExplicit()) {
            DependentLink param = result.getParameter();
            if (param.hasNext() && !param.isExplicit()) {
              break;
            }
          }
          result = inferArg(result, argument.expression, argument.isExplicit(), fun);
        }

        if (result == null || i == arguments.size()) {
          return result;
        }

        Pair<Expression, Integer> pair = normalizePi(arguments, i, result.getType());
        ((TypecheckingResult) result).type = pair.proj1;

        for (; i < pair.proj2; i++) {
          Concrete.Argument argument = arguments.get(i);
          result = inferArg(result, argument.expression, argument.isExplicit(), fun);
        }

        if (result == null) {
          return null;
        }
        if (i < arguments.size()) {
          result = fixImplicitArgs(result, result.getImplicitParameters(), fun, false, null);
          Expression actualType = dropPiParameters(result.getType(), arguments, i);
          if (actualType != null) {
            new CompareVisitor(myVisitor.getEquations(), CMP.LE, fun).compare(actualType, expectedType, Type.OMEGA, false);
          }
        }
      }

      for (; i < arguments.size(); i++) {
        Concrete.Argument argument = arguments.get(i);
        result = inferArg(result, argument.expression, argument.isExplicit(), fun);
      }
    }

    return result;
  }

  private static Expression generateAppExpressionByArguments(Expression rootFunction, List<@NotNull TypecheckingResult> argumentResults, List<Concrete.@NotNull Argument> arguments) {
    assert argumentResults.size() == arguments.size();
    Expression actualFunction;
    if (arguments.size() == 1) {
      actualFunction = rootFunction;
    } else {
      actualFunction = generateAppExpressionByArguments(rootFunction, argumentResults.subList(0, argumentResults.size() - 1), arguments.subList(0, arguments.size() - 1));
    }
    return AppExpression.make(actualFunction, argumentResults.get(argumentResults.size() - 1).expression, arguments.get(argumentResults.size() - 1).isExplicit());
  }

  private PiExpression generatePiExpressionByArguments(@NotNull Expression codomain,  List<@NotNull TypecheckingResult> argumentResults, List<Concrete.@NotNull Argument> arguments, Concrete.SourceNode node) {
    Expression actualCodomain;
    if (argumentResults.size() == 1) {
      actualCodomain = codomain;
    } else {
      actualCodomain = generatePiExpressionByArguments(codomain, argumentResults.subList(1, argumentResults.size()), arguments.subList(1, arguments.size()), node);
    }
    var domain = argumentResults.get(0).type;
    Type type = domain instanceof Type ? (Type) domain : new TypeExpression(domain, domain.getSortOfType());
    return new PiExpression(PiExpression.generateUpperBound(domain.getSortOfType(), actualCodomain.getSortOfType(), myVisitor.getEquations(), node), new TypedSingleDependentLink(arguments.get(0).isExplicit(), null, type), actualCodomain);
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
      loop:
      for (; param.hasNext() && i < arguments.size(); param = param.getNext(), i++) {
        while (param.isExplicit() != arguments.get(i).isExplicit()) {
          param = param.getNext();
          if (!param.hasNext()) break loop;
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
        if (!result1.type.reportIfError(myVisitor.getErrorReporter(), expr)) {
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
    return new ExpressionInferenceVariable(expectedType, sourceNode, myVisitor.getAllBindings(), true);
  }
}
