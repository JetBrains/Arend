package org.arend.typechecking.doubleChecker;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.definition.*;
import org.arend.core.elimtree.Body;
import org.arend.core.elimtree.ElimBody;
import org.arend.core.elimtree.IntervalElim;
import org.arend.core.expr.*;
import org.arend.core.expr.type.Type;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.Levels;
import org.arend.ext.core.definition.CoreFunctionDefinition;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.TypeMismatchError;
import org.arend.ext.error.TypecheckingError;
import org.arend.prelude.Prelude;
import org.arend.typechecking.UseTypechecking;
import org.arend.typechecking.error.local.CertainTypecheckingError;
import org.arend.typechecking.error.local.CoreErrorWrapper;
import org.arend.ext.error.ArgInferenceError;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.arend.typechecking.patternmatching.PatternTypechecking;
import org.arend.typechecking.visitor.BaseDefinitionTypechecker;
import org.arend.typechecking.visitor.DefinitionTypechecker;

import java.util.*;

public class CoreDefinitionChecker extends BaseDefinitionTypechecker {
  private final CoreExpressionChecker myChecker;

  public CoreDefinitionChecker(ErrorReporter errorReporter) {
    super(errorReporter);
    myChecker = new CoreExpressionChecker(new HashSet<>(), DummyEquations.getInstance(), null);
  }

  void setErrorReporter(ErrorReporter errorReporter) {
    this.errorReporter = errorReporter;
  }

  public boolean check(Definition definition) {
    myChecker.clear();
    myChecker.setDefinition(definition);
    try {
      myChecker.checkDependentLink(definition.getParameters(), Type.OMEGA, null);

      // TODO[double_check]: Check (mutual) recursion
      // TODO[double_check]: Check definition.hasUniverses()
      // TODO[double_check]: Check definition.getParametersLevels()

      if (definition instanceof FunctionDefinition) {
        return check((FunctionDefinition) definition);
      } else if (definition instanceof DataDefinition) {
        return check((DataDefinition) definition);
      } else if (definition instanceof ClassDefinition) {
        return check((ClassDefinition) definition);
      } else if (definition instanceof MetaTopDefinition) {
        return check((MetaTopDefinition) definition);
      } else {
        throw new IllegalStateException();
      }
    } catch (CoreException e) {
      errorReporter.report(e.error);
      return false;
    }
  }

  private boolean check(FunctionDefinition definition) {
    Body body = definition.getReallyActualBody();
    boolean checkType = true;
    if (body instanceof NewExpression && ((NewExpression) body).getRenewExpression() == null && definition.getResultType() instanceof ClassCallExpression typeClassCall && definition.getResultTypeLevel() == null) {
      Map<ClassField, Expression> newImpls = new LinkedHashMap<>();
      ClassCallExpression bodyClassCall = ((NewExpression) body).getClassCall();
      ClassCallExpression newClassCall = new ClassCallExpression(bodyClassCall.getDefinition(), typeClassCall.getLevels(), newImpls, Sort.PROP, UniverseKind.NO_UNIVERSES);
      Expression newThisBinding = new ReferenceExpression(newClassCall.getThisBinding());
      boolean ok = true;
      for (ClassField field : typeClassCall.getDefinition().getFields()) {
        Expression typeImpl = typeClassCall.getAbsImplementationHere(field);
        Expression bodyImpl = bodyClassCall.getAbsImplementationHere(field);
        if (typeImpl != null && bodyImpl != null) {
          ok = false;
          break;
        }
        if (typeImpl != null || bodyImpl != null) {
          newImpls.put(field, typeImpl != null ? typeImpl.subst(typeClassCall.getThisBinding(), newThisBinding) : bodyImpl.subst(bodyClassCall.getThisBinding(), newThisBinding));
        }
      }
      if (ok) {
        checkType = false;
        body = new NewExpression(null, newClassCall);
      }
    }

    Expression typeType = checkType ? definition.getResultType().accept(myChecker, Type.OMEGA) : null;
    Integer level = definition.getResultTypeLevel() == null ? null : myChecker.checkLevelProof(definition.getResultTypeLevel(), definition.getResultType());

    if (definition.getKind() == CoreFunctionDefinition.Kind.LEMMA && (level == null || level != -1)) {
      if (!DefinitionTypechecker.isBoxed(definition)) {
        DefCallExpression resultDefCall = definition.getResultType().cast(DefCallExpression.class);
        if (resultDefCall == null || !Objects.equals(resultDefCall.getUseLevel(), -1)) {
          Sort sort = typeType == null ? definition.getResultType().getSortOfType() : typeType.toSort();
          if (sort == null) {
            errorReporter.report(CoreErrorWrapper.make(new TypecheckingError("Cannot infer the sort of the type", null), definition.getResultType()));
            return false;
          }
          if (!sort.isProp()) {
            errorReporter.report(CoreErrorWrapper.make(new TypeMismatchError(new UniverseExpression(Sort.PROP), new UniverseExpression(sort), null), definition.getResultType()));
            return false;
          }
        }
      }
    }

    if (definition.isAxiom()) {
      if (body != null) {
        errorReporter.report(new CertainTypecheckingError(CertainTypecheckingError.Kind.AXIOM_WITH_BODY, null));
        return false;
      } else {
        return true;
      }
    }

    if (body instanceof Expression) {
      if (body instanceof CaseExpression) {
        myChecker.checkCase((CaseExpression) body, definition.getResultType(), level);
      } else {
        ((Expression) body).accept(myChecker, checkType ? definition.getResultType() : null);
      }
      return true;
    }

    ElimBody elimBody;
    if (body instanceof IntervalElim intervalElim) {
      if (intervalElim.getCases().isEmpty()) {
        errorReporter.report(new TypecheckingError("Empty IntervalElim", null));
        return false;
      }

      int offset = intervalElim.getOffset();
      DependentLink link = definition.getParameters();
      for (int i = 0; i < offset && link.hasNext(); i++) {
        link = link.getNext();
      }

      for (IntervalElim.CasePair ignored : intervalElim.getCases()) {
        if (!link.hasNext()) {
          errorReporter.report(new TypecheckingError("Interval elim has too many parameters", null));
          return false;
        }

        DataCallExpression dataCall = link.getTypeExpr().normalize(NormalizationMode.WHNF).cast(DataCallExpression.class);
        if (!(dataCall != null && dataCall.getDefinition() == Prelude.INTERVAL)) {
          errorReporter.report(new TypeMismatchError(DataCallExpression.make(Prelude.INTERVAL, Levels.EMPTY, Collections.emptyList()), link.getTypeExpr(), null));
          return false;
        }

        link = link.getNext();
      }

      // TODO[double_check]: Check interval conditions

      if (intervalElim.getOtherwise() == null) {
        errorReporter.report(new TypecheckingError("Missing non-interval clauses", null));
        return false;
      }

      elimBody = intervalElim.getOtherwise();
    } else if (body instanceof ElimBody) {
      elimBody = (ElimBody) body;
    } else if (body == null) {
      ClassCallExpression classCall = definition.getResultType().normalize(NormalizationMode.WHNF).cast(ClassCallExpression.class);
      if (classCall == null) {
        errorReporter.report(new TypecheckingError("Missing a body", null));
        return false;
      }
      myChecker.checkCocoverage(classCall);
      return true;
    } else {
      throw new IllegalStateException();
    }

    myChecker.checkElimBody(definition, elimBody, definition.getParameters(), definition.getResultType(), level, null, definition.isSFunc(), PatternTypechecking.Mode.FUNCTION);
    return true;
  }

  private boolean check(DataDefinition definition) {
    myChecker.clear();

    if (definition.getParameters().hasNext()) {
      Set<DependentLink> parameters = new HashSet<>();
      for (DependentLink link = definition.getParameters(); link.hasNext(); link = link.getNext()) {
        parameters.add(link);
      }
      getCovariantParameters(definition, parameters);
      int index = 0;
      for (DependentLink link = definition.getParameters(); link.hasNext(); link = link.getNext()) {
        if (definition.isCovariant(index) && !parameters.contains(link)) {
          errorReporter.report(new TypecheckingError(ArgInferenceError.ordinal(index) + " parameter is not covariant", null));
          return false;
        }
        index++;
      }
    }

    if (!definition.isTruncated() && definition.getSquasher() == null) {
      for (Constructor constructor : definition.getConstructors()) {
        if (constructor.getBody() instanceof IntervalElim && !definition.getSort().getHLevel().isInfinity()) {
          errorReporter.report(new TypecheckingError("A higher inductive type must have sort " + new Sort(new Level(LevelVariable.PVAR), Level.INFINITY), null));
          return false;
        }
      }
    }

    for (Constructor constructor : definition.getConstructors()) {
      if (constructor.getDataType() != definition) {
        errorReporter.report(new TypecheckingError("Constructor '" + constructor.getName() + "' belongs to '" + definition.getName() + "', but its data type is '" + constructor.getDataType().getName() + "'", null));
        return false;
      }

      // TODO[double_check]: Check patterns

      myChecker.addDependentLink(constructor.getDataTypeParameters());
      Sort sort = myChecker.checkDependentLink(constructor.getParameters(), null);
      myChecker.freeDependentLink(constructor.getParameters());
      myChecker.freeDependentLink(constructor.getDataTypeParameters());

      if (!checkDefinitionSort(definition.isTruncated() || definition.getSquasher() != null, constructor, sort, definition.getSort())) {
        return false;
      }

      // TODO[double_check]: Check clauses/body
    }

    return checkSquasher(definition.getSquasher(), definition, definition.getSort());
  }

  private boolean check(MetaTopDefinition definition) {
    return true;
  }

  private boolean checkSquasher(FunctionDefinition squasher, Definition definition, Sort sort) {
    if (squasher == null) {
      return true;
    }

    ParametersLevel parametersLevel = UseTypechecking.typecheckLevel(null, squasher, definition, errorReporter);
    if (parametersLevel == null) {
      return false;
    }
    if (parametersLevel.parameters != null) {
      errorReporter.report(new TypecheckingError("\\use \\level " + squasher.getName() + " applies only to specific parameters", null));
      return false;
    }

    Level hLevel = new Level(parametersLevel.level);
    if (!Level.compare(hLevel, sort.getHLevel(), CMP.LE, DummyEquations.getInstance(), null)) {
      errorReporter.report(new TypecheckingError("The h-level " + sort.getHLevel() + " of '" + definition.getName() + "' does not fit into the h-level " + hLevel + " of \\use \\level " + squasher.getName(), null));
      return false;
    }

    return true;
  }

  private boolean checkDefinitionSort(boolean isSquashed, Definition definition, Sort defSort, Sort expectedSort) {
    boolean ok;
    if (isSquashed) {
      ok = expectedSort.isProp() || Level.compare(defSort.getPLevel(), expectedSort.getPLevel(), CMP.LE, DummyEquations.getInstance(), null);
    } else {
      ok = defSort.isLessOrEquals(expectedSort);
    }
    if (!ok) {
      errorReporter.report(new TypecheckingError("The sort " + defSort + " of '" + definition.getName() + "' does not fit into the expected sort " + expectedSort, null));
      return false;
    }
    return true;
  }

  private boolean visitClass(ClassDefinition classDef, Set<ClassDefinition> stack, Set<ClassDefinition> visited, List<ClassField> fields) {
    if (!visited.add(classDef)) {
      return true;
    }
    if (!stack.add(classDef)) {
      errorReporter.report(new TypecheckingError("Class '" + classDef.getName() + "' depends recursively on its super classes", null));
      return false;
    }

    for (ClassDefinition superClass : classDef.getSuperClasses()) {
      visitClass(superClass, stack, visited, fields);
    }

    fields.addAll(classDef.getPersonalFields());

    stack.remove(classDef);
    return true;
  }

  private boolean check(ClassDefinition definition) {
    List<ClassField> fields = new ArrayList<>();
    if (!visitClass(definition, new HashSet<>(), new HashSet<>(), fields)) {
      return false;
    }
    if (!fields.equals(new ArrayList<>(definition.getFields()))) {
      errorReporter.report(new TypecheckingError("Class '" + definition.getName() + "' should have fields " + fields + ", but has fields " + definition.getFields(), null));
      return false;
    }

    for (Map.Entry<ClassDefinition, Levels> entry : definition.getSuperLevels().entrySet()) {
      myChecker.checkLevels(entry.getValue(), entry.getKey(), null);
    }

    for (ClassField field : definition.getPersonalFields()) {
      if (field.getParentClass() != definition) {
        errorReporter.report(new TypecheckingError("Field '" + field.getName() + "' belongs to '" + definition.getName() + "', but its class is '" + field.getParentClass().getName() + "'", null));
        return false;
      }

      PiExpression fieldType = definition.getFieldType(field);
      myChecker.addBinding(fieldType.getParameters(), fieldType.getCodomain());
      Expression typeType = fieldType.getCodomain().accept(myChecker, Type.OMEGA);
      myChecker.removeBinding(fieldType.getParameters());

      Integer level;
      if (field.getTypeLevel() != null) {
        List<DependentLink> parameters = new ArrayList<>();
        Expression type = fieldType;
        int sum = field.getNumberOfParameters();
        for (int i = 0; i < sum; ) {
          if (!(type instanceof PiExpression piType)) {
            errorReporter.report(new TypecheckingError("The type of field '" + field.getName() + "' should have at least " + sum + " parameters, but has only " + i, null));
            return false;
          }
          SingleDependentLink link = piType.getParameters();
          for (; link.hasNext() && i < sum; link = link.getNext(), i++) {
            parameters.add(link);
          }
          type = piType.getCodomain();
          if (link.hasNext()) {
            type = new PiExpression(piType.getResultSort(), link, type);
          }
        }

        for (DependentLink parameter : parameters) {
          myChecker.addBinding(parameter, null);
        }
        level = myChecker.checkLevelProof(field.getTypeLevel(), type);
        for (DependentLink parameter : parameters) {
          myChecker.removeBinding(parameter);
        }
      } else {
        level = null;
      }
      if (typeType == null) {
        return false;
      }

      Sort sort = typeType.toSort();
      if (sort == null) {
        errorReporter.report(CoreErrorWrapper.make(new TypecheckingError("Cannot infer the sort of the type of field '" + field.getName() + "'", null), fieldType));
        return false;
      }

      if (!definition.isImplemented(field) && !checkDefinitionSort(definition.getSquasher() != null, field, sort, definition.getSort())) {
        return false;
      }

      boolean propertyOK = !field.isProperty();
      if (level != null) {
        if (field.isProperty() && level == -1) {
          propertyOK = true;
        }
        if (field.getResultTypeLevel() != level) {
          errorReporter.report(CoreErrorWrapper.make(new TypecheckingError("The level (" + field.getResultTypeLevel() + ") of the type of the field does not match the level (" + level + ") inferred from the proof", null), fieldType));
        }
      }

      if (!propertyOK) {
        DefCallExpression defCall = fieldType.getCodomain().cast(DefCallExpression.class);
        if (defCall != null) {
          Integer defCallLevel = defCall.getUseLevel();
          propertyOK = defCallLevel != null && defCallLevel == -1;
        }
      }

      if (!propertyOK && !sort.isProp()) {
        errorReporter.report(new TypecheckingError("The level of property '" + field.getName() + "' must be \\Prop", null));
        return false;
      }

      // TODO[double_check]: Check covariance
    }

    if (!checkSquasher(definition.getSquasher(), definition, definition.getSort())) {
      return false;
    }

    // TODO[double_check]: Check occurrences of fields in other fields
    // TODO[double_check]: Check implemented
    // TODO[double_check]: Check overridden

    return true;
  }
}
