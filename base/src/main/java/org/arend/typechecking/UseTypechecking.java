package org.arend.typechecking;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.*;
import org.arend.core.expr.*;
import org.arend.core.expr.type.Type;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.concrete.definition.FunctionKind;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.CertainTypecheckingError;
import org.arend.typechecking.error.local.CoerceCycleError;
import org.arend.typechecking.error.local.LocalErrorReporter;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.arend.typechecking.order.DFS;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.arend.ext.util.Pair;

import java.util.*;

public class UseTypechecking {
  public static void typecheck(List<Concrete.UseDefinition> definitions, ErrorReporter errorReporter) {
    Map<Definition, List<Pair<Expression,FunctionDefinition>>> fromMap = new HashMap<>();
    Map<Definition, List<Pair<Expression,FunctionDefinition>>> toMap = new HashMap<>();

    for (Concrete.UseDefinition definition : definitions) {
      Definition typedDefinition = definition.getData().getTypechecked();
      if (!(typedDefinition instanceof FunctionDefinition)) {
        continue;
      }

      FunctionDefinition useDefinition = (FunctionDefinition) typedDefinition;
      ErrorReporter localErrorReporter = new LocalErrorReporter(definition.getData(), errorReporter);
      if (definition.getKind() == FunctionKind.LEVEL && !useDefinition.getResultType().isError()) {
        Definition useParent = definition.getUseParent().getTypechecked();
        ParametersLevel parametersLevel = typecheckLevel(definition, useDefinition, useParent, localErrorReporter);
        if (parametersLevel != null) {
          registerParametersLevel(useDefinition, useParent, parametersLevel);
        }
      } else if (definition.getKind() == FunctionKind.COERCE) {
        typecheckCoerce(definition, useDefinition, localErrorReporter, fromMap, toMap);
      }
    }

    registerCoerce(fromMap, true, errorReporter, definitions);
    registerCoerce(toMap, false, errorReporter, definitions);
  }

  private static void registerCoerce(Map<Definition, List<Pair<Expression,FunctionDefinition>>> depMap, boolean isFrom, ErrorReporter errorReporter, List<Concrete.UseDefinition> definitions) {
    if (depMap.isEmpty()) {
      return;
    }

    List<Definition> order = new ArrayList<>();
    try {
      DFS<Definition,Void> dfs = new DFS<>() {
        @Override
        protected boolean allowCycles() {
          return false;
        }

        @Override
        protected Void forDependencies(Definition unit) {
          List<Pair<Expression, FunctionDefinition>> deps = depMap.get(unit);
          if (deps != null) {
            for (Pair<Expression, FunctionDefinition> dep : deps) {
              DefCallExpression defCall = dep.proj1 != null ? dep.proj1.cast(DefCallExpression.class) : null;
              if (defCall != null) {
                visit(defCall.getDefinition());
              }
            }
          }
          order.add(unit);
          return null;
        }
      };
      for (Definition definition : depMap.keySet()) {
        dfs.visit(definition);
      }
    } catch (DFS.CycleException e) {
      List<Concrete.UseDefinition> coerceDefs = new ArrayList<>();
      for (Concrete.UseDefinition definition : definitions) {
        if (definition.getKind() == FunctionKind.COERCE) {
          coerceDefs.add(definition);
        }
      }
      errorReporter.report(new CoerceCycleError(coerceDefs));
      return;
    }

    for (Definition definition : order) {
      List<Pair<Expression, FunctionDefinition>> deps = depMap.get(definition);
      if (deps != null) {
        CoerceData coerceData = definition.getCoerceData();
        for (Pair<Expression, FunctionDefinition> dep : deps) {
          if (isFrom) {
            coerceData.addCoerceFrom(dep.proj1, dep.proj2);
          } else {
            coerceData.addCoerceTo(dep.proj1, dep.proj2);
          }
        }
      }
    }
  }

  private static void typecheckCoerce(Concrete.UseDefinition def, FunctionDefinition typedDef, ErrorReporter errorReporter, Map<Definition, List<Pair<Expression,FunctionDefinition>>> fromMap, Map<Definition, List<Pair<Expression,FunctionDefinition>>> toMap) {
    Definition useParent = def.getUseParent().getTypechecked();
    if ((useParent instanceof DataDefinition || useParent instanceof ClassDefinition) && !def.getParameters().isEmpty()) {
      DependentLink lastParam = DependentLink.Helper.getLast(typedDef.getParameters());
      Expression paramType = lastParam.hasNext() ? lastParam.getTypeExpr() : null;
      DefCallExpression paramDefCall = paramType == null ? null : paramType.cast(DefCallExpression.class);
      Definition paramDef = paramDefCall == null ? null : paramDefCall.getDefinition();
      DefCallExpression resultDefCall = typedDef.getResultType() == null ? null : typedDef.getResultType().cast(DefCallExpression.class);
      Definition resultDef = resultDefCall == null ? null : resultDefCall.getDefinition();

      if ((resultDef == useParent) == (paramDef == useParent)) {
        if (!(typedDef.getResultType() instanceof ErrorExpression || typedDef.getResultType() == null)) {
          errorReporter.report(new TypecheckingError("Either the last parameter or the result type (but not both) of \\coerce must be the parent definition", def));
        }
      } else {
        typedDef.setVisibleParameter(DependentLink.Helper.size(typedDef.getParameters()) - 1);
        if (resultDef == useParent) {
          fromMap.computeIfAbsent(useParent, k -> new ArrayList<>()).add(new Pair<>(paramType, typedDef));
        } else {
          toMap.computeIfAbsent(useParent, k -> new ArrayList<>()).add(new Pair<>(typedDef.getResultType(), typedDef));
        }
      }
    }
  }

  public static ParametersLevel typecheckLevel(Concrete.UseDefinition def, FunctionDefinition typedDef, Definition useParent, ErrorReporter errorReporter) {
    if (!(useParent instanceof DataDefinition || useParent instanceof ClassDefinition || useParent instanceof FunctionDefinition)) {
      return null;
    }

    Expression resultType = def == null || def.getResultType() != null ? typedDef.getResultType() : null;
    boolean ok = true;
    List<ClassField> levelFields = null;
    List<Pair<ClassDefinition, Set<ClassField>>> strictList = null;
    Expression type = null;
    DependentLink parameters = null;
    DependentLink link = typedDef.getParameters();
    if (useParent instanceof DataDefinition || useParent instanceof FunctionDefinition) {
      ExprSubstitution substitution = new ExprSubstitution();
      List<Expression> defCallArgs = new ArrayList<>();
      for (DependentLink defLink = useParent.getParameters(); defLink.hasNext(); defLink = defLink.getNext(), link = link.getNext()) {
        if (!link.hasNext()) {
          ok = false;
          break;
        }
        if (!Expression.compare(link.getTypeExpr(), defLink.getTypeExpr().subst(substitution), Type.OMEGA, CMP.EQ)) {
          if (parameters == null) {
            parameters = DependentLink.Helper.take(typedDef.getParameters(), DependentLink.Helper.size(defLink));
          }
        }
        ReferenceExpression refExpr = new ReferenceExpression(link);
        defCallArgs.add(refExpr);
        substitution.add(defLink, refExpr);
      }

      if (ok) {
        if (link.hasNext() || resultType != null) {
          type = useParent instanceof DataDefinition
            ? DataCallExpression.make((DataDefinition) useParent, useParent.makeIdLevels(), defCallArgs)
            : FunCallExpression.make((FunctionDefinition) useParent, useParent.makeIdLevels(), defCallArgs);
        } else {
          ok = false;
        }
      }
    } else {
      ClassCallExpression classCall = null;
      DependentLink classCallLink = link;
      for (; classCallLink.hasNext(); classCallLink = classCallLink.getNext()) {
        classCallLink = classCallLink.getNextTyped(null);
        classCall = classCallLink.getTypeExpr().cast(ClassCallExpression.class);
        if (classCall != null && classCall.getDefinition() == useParent && (classCall.getUniverseKind() == UniverseKind.NO_UNIVERSES || typedDef.isIdLevels(classCall.getLevels()))) {
          break;
        }
      }
      if (!classCallLink.hasNext() && resultType != null) {
        PiExpression piType = resultType.normalize(NormalizationMode.WHNF).cast(PiExpression.class);
        if (piType != null) {
          classCall = piType.getParameters().getTypeExpr().normalize(NormalizationMode.WHNF).cast(ClassCallExpression.class);
          if (classCall != null && classCall.getDefinition() == useParent && (classCall.getUniverseKind() == UniverseKind.NO_UNIVERSES || typedDef.isIdLevels(classCall.getLevels()))) {
            classCallLink = piType.getParameters();
          }
        }
      }

      if (classCall == null || !classCallLink.hasNext()) {
        ok = false;
      } else {
        if (!classCall.getImplementedHere().isEmpty()) {
          levelFields = new ArrayList<>();
          strictList = new ArrayList<>();
          Expression thisExpr = new ReferenceExpression(classCallLink);
          for (Map.Entry<ClassField, Expression> entry : classCall.getImplementedHere().entrySet()) {
            ClassField classField = entry.getKey();
            if (classField.isProperty()) {
              continue;
            }
            if (!(link.hasNext() && entry.getValue() instanceof ReferenceExpression && ((ReferenceExpression) entry.getValue()).getBinding() == link)) {
              ok = false;
              break;
            }
            levelFields.add(classField);
            Expression fieldType = classCall.getDefinition().getFieldType(classField, classCall.getLevels(classField.getParentClass()), thisExpr);
            Expression paramType = link.getTypeExpr();
            if (!Expression.compare(fieldType, paramType, Type.OMEGA, CMP.EQ)) {
              if (parameters == null) {
                int numberOfClassParameters = 0;
                for (DependentLink link1 = link; link1 != classCallLink && link1.hasNext(); link1 = link1.getNext()) {
                  numberOfClassParameters++;
                }
                parameters = DependentLink.Helper.take(typedDef.getParameters(), numberOfClassParameters);
              }

              ClassCallExpression fieldClassCall = fieldType.cast(ClassCallExpression.class);
              ClassCallExpression paramClassCall = paramType.cast(ClassCallExpression.class);
              if (strictList != null && paramClassCall != null && fieldClassCall != null && paramClassCall.getDefinition().isSubClassOf(fieldClassCall.getDefinition()) && paramClassCall.getLevels(fieldClassCall.getDefinition()).equals(fieldClassCall.getLevels()) && paramClassCall.getUniverseKind().ordinal() <= fieldClassCall.getUniverseKind().ordinal()) {
                strictList.add(new Pair<>(paramClassCall.getDefinition(), paramClassCall.getImplementedHere().keySet()));
              } else {
                strictList = null;
              }
            } else if (strictList != null) {
              strictList.add(null);
            }
            link = link.getNext();
          }
        }
        type = classCall;
      }
    }

    Integer level = CheckTypeVisitor.getExpressionLevel(link, resultType, ok ? type : null, DummyEquations.getInstance(), def, errorReporter);
    if (level == null) {
      return null;
    }
    if (def != null && useParent instanceof DataDefinition && parameters == null && Level.compare(((DataDefinition) useParent).getSort().getHLevel(), new Level(level), CMP.LE, DummyEquations.getInstance(), def)) {
      errorReporter.report(new CertainTypecheckingError(CertainTypecheckingError.Kind.USELESS_LEVEL, def));
    }

    return useParent instanceof ClassDefinition ? new ClassDefinition.ParametersLevel(parameters, level, levelFields, strictList) : new ParametersLevel(parameters, level);
  }

  private static void registerParametersLevel(FunctionDefinition useDefinition, Definition useParent, ParametersLevel parametersLevel) {
    if (useParent instanceof DataDefinition) {
      DataDefinition dataDef = (DataDefinition) useParent;
      if (parametersLevel.parameters == null) {
        Sort newSort = parametersLevel.level == -1 ? Sort.PROP : new Sort(dataDef.getSort().getPLevel(), new Level(parametersLevel.level));
        if (!dataDef.getSort().isLessOrEquals(newSort)) {
          if (!(parametersLevel.level == -1 && dataDef.getSort().isSet())) {
            dataDef.setSquashed(true);
          }
          dataDef.setSquasher(useDefinition);
          dataDef.setSort(newSort);
        }
      } else {
        dataDef.addParametersLevel(parametersLevel);
      }
    } else if (useParent instanceof FunctionDefinition) {
      ((FunctionDefinition) useParent).addParametersLevel(parametersLevel);
    } else {
      ClassDefinition classDef = (ClassDefinition) useParent;
      ClassDefinition.ParametersLevel classParametersLevel = (ClassDefinition.ParametersLevel) parametersLevel;
      if (classParametersLevel.fields == null) {
        classDef.setSquasher(useDefinition);
        classDef.setSort(parametersLevel.level == -1 ? Sort.PROP : new Sort(classDef.getSort().getPLevel(), new Level(parametersLevel.level)));
      } else {
        classDef.addParametersLevel(classParametersLevel);
      }
    }
  }
}
