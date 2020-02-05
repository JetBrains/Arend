package org.arend.typechecking;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.definition.*;
import org.arend.core.elimtree.Body;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.elimtree.IntervalElim;
import org.arend.core.expr.*;
import org.arend.core.expr.type.Type;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.ext.core.definition.CoreFunctionDefinition;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.TypeMismatchError;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.arend.prelude.Prelude;
import org.arend.typechecking.error.local.CertainTypecheckingError;
import org.arend.typechecking.error.local.CoreErrorWrapper;
import org.arend.typechecking.error.local.inference.ArgInferenceError;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.arend.typechecking.visitor.BaseDefinitionTypechecker;

import java.util.*;

public class CoreDefinitionChecker extends BaseDefinitionTypechecker {
  private final CoreExpressionChecker myChecker;

  public CoreDefinitionChecker(ErrorReporter errorReporter) {
    super(errorReporter);
    myChecker = new CoreExpressionChecker(errorReporter, new HashSet<>(), DummyEquations.getInstance(), null);
  }

  public boolean check(Definition definition) {
    myChecker.clear();
    if (!myChecker.checkDependentLink(definition.getParameters(), Type.OMEGA, null)) {
      return false;
    }

    // TODO[double_check]: Check (mutual) recursion
    // TODO[double_check]: Check definition.hasUniverses()
    // TODO[double_check]: Check definition.getParametersLevels()

    if (definition instanceof FunctionDefinition) {
      return check((FunctionDefinition) definition);
    } else if (definition instanceof DataDefinition) {
      return check((DataDefinition) definition);
    } else if (definition instanceof ClassDefinition) {
      return check((ClassDefinition) definition);
    } else {
      throw new IllegalStateException();
    }
  }

  private boolean check(FunctionDefinition definition) {
    Expression typeType = definition.getResultType().accept(myChecker, Type.OMEGA);
    if (typeType == null) {
      return false;
    }

    Integer level;
    if (definition.getResultTypeLevel() != null) {
      level = myChecker.checkLevelProof(definition.getResultTypeLevel(), definition.getResultType());
      if (level == null) {
        return false;
      }
    } else {
      level = null;
    }

    if (definition.getKind() == CoreFunctionDefinition.Kind.LEMMA && (level == null || level != -1)) {
      DefCallExpression resultDefCall = definition.getResultType().cast(DefCallExpression.class);
      if (resultDefCall == null || !Objects.equals(resultDefCall.getUseLevel(), -1)) {
        Sort sort = typeType.toSort();
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

    Body body = definition.getActualBody();
    if (body instanceof Expression) {
      return ((Expression) body).accept(myChecker, definition.getResultType()) != null;
    }

    if (body instanceof IntervalElim) {
      IntervalElim intervalElim = (IntervalElim) body;
      if (intervalElim.getCases().isEmpty()) {
        errorReporter.report(new TypecheckingError("Empty IntervalElim", null));
        return false;
      }

      int offset = intervalElim.getOffset();
      DependentLink link = definition.getParameters();
      for (int i = 0; i < offset && link.hasNext(); i++) {
        link = link.getNext();
      }

      for (IntervalElim.CasePair casePair : intervalElim.getCases()) {
        if (!link.hasNext()) {
          errorReporter.report(new TypecheckingError("Interval elim has too many parameters", null));
          return false;
        }

        DataCallExpression dataCall = link.getTypeExpr().normalize(NormalizationMode.WHNF).cast(DataCallExpression.class);
        if (!(dataCall != null && dataCall.getDefinition() == Prelude.INTERVAL)) {
          errorReporter.report(new TypeMismatchError(new DataCallExpression(Prelude.INTERVAL, Sort.PROP, Collections.emptyList()), link.getTypeExpr(), null));
          return false;
        }

        link = link.getNext();
      }

      // TODO[double_check]: Check interval conditions

      return myChecker.checkElimTree(((IntervalElim) body).getOtherwise(), null, definition.isSFunc());
    } else if (body instanceof ElimTree) {
      return myChecker.checkElimTree((ElimTree) body, null, definition.isSFunc());
    } else if (body == null) {
      ClassCallExpression classCall = definition.getResultType().cast(ClassCallExpression.class);
      if (classCall == null) {
        errorReporter.report(new TypeMismatchError(DocFactory.text("a classCall"), definition.getResultType(), null));
        return false;
      }
      return myChecker.checkCocoverage(classCall);
    } else {
      throw new IllegalStateException();
    }
  }

  private boolean check(DataDefinition definition) {
    myChecker.clear();

    int index = 0;
    for (DependentLink link = definition.getParameters(); link.hasNext(); link = link.getNext()) {
      if (definition.isCovariant(index) && !isCovariantParameter(definition, link)) {
        errorReporter.report(new TypecheckingError(ArgInferenceError.ordinal(index) + " parameter is not covariant", null));
        return false;
      }
      index++;
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
      if (sort == null) {
        return false;
      }

      boolean ok;
      if (definition.isTruncated() || definition.getSquasher() != null) {
        ok = definition.getSort().isProp() || Level.compare(sort.getPLevel(), definition.getSort().getPLevel(), CMP.LE, DummyEquations.getInstance(), null);
      } else {
        ok = sort.isLessOrEquals(definition.getSort());
      }
      if (!ok) {
        errorReporter.report(new TypecheckingError("The sort " + sort + " of constructor '" + constructor.getName() + "' does not fit into the sort " + definition.getSort() + " of its data type", null));
        return false;
      }

      // TODO[double_check]: Check clauses/body
    }

    if (definition.getSquasher() != null) {
      ParametersLevel parametersLevel = UseTypechecking.typecheckLevel(null, definition.getSquasher(), definition, errorReporter);
      if (parametersLevel == null) {
        return false;
      }
      if (parametersLevel.parameters != null) {
        errorReporter.report(new TypecheckingError("\\use \\level " + definition.getSquasher().getName() + " applies only to specific parameters", null));
        return false;
      }

      Level hLevel = new Level(parametersLevel.level);
      if (!Level.compare(hLevel, definition.getSort().getHLevel(), CMP.LE, DummyEquations.getInstance(), null)) {
        errorReporter.report(new TypecheckingError("The h-level " + definition.getSort().getHLevel() + " of data type '" + definition.getName() + "' does not fit into the h-level " + hLevel + " of the squashing function", null));
        return false;
      }
    }

    return true;
  }

  private boolean visitClass(ClassDefinition classDef, Set<ClassDefinition> stack, Set<ClassDefinition> visited, List<ClassField> fields) {
    if (!stack.add(classDef)) {
      errorReporter.report(new TypecheckingError("Class '" + classDef.getName() + "' depends recursively on its super classes", null));
      return false;
    }
    if (!visited.add(classDef)) {
      return true;
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

    for (ClassField field : definition.getPersonalFields()) {
      if (field.getParentClass() != definition) {
        errorReporter.report(new TypecheckingError("Field '" + field.getName() + "' belongs to '" + definition.getName() + "', but its class is '" + field.getParentClass().getName() + "'", null));
        return false;
      }

      PiExpression fieldType = field.getType(Sort.STD);
      myChecker.addBinding(fieldType.getParameters(), fieldType.getCodomain());
      Expression typeType = fieldType.getCodomain().accept(myChecker, Type.OMEGA);
      Integer level;
      if (field.getTypeLevel() != null) {
        List<DependentLink> parameters = new ArrayList<>();
        Expression type = fieldType.getCodomain();
        int sum = field.getNumberOfParameters();
        for (int i = 0; i < sum; ) {
          if (!(type instanceof PiExpression)) {
            errorReporter.report(new TypecheckingError("The type of field '" + field.getName() + "' should have at least " + sum + " parameters, but has only " + i, null));
            return false;
          }
          PiExpression piType = (PiExpression) type;
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
        if (level == null) {
          return false;
        }
      } else {
        level = null;
      }
      myChecker.removeBinding(fieldType.getParameters());
      if (typeType == null) {
        return false;
      }

      Sort sort = typeType.toSort();
      if (sort == null) {
        errorReporter.report(CoreErrorWrapper.make(new TypecheckingError("Cannot infer the sort of the type of field '" + field.getName() + "'", null), fieldType));
        return false;
      }

      if (!sort.isLessOrEquals(definition.getSort())) {
        errorReporter.report(CoreErrorWrapper.make(new TypecheckingError("The sort " + sort + " of field '" + field.getName() + "' does not fit into the sort " + definition.getSort() + " of its class", null), fieldType));
        return false;
      }

      boolean propertyOK = !field.isProperty();
      if (level != null) {
        if (field.isProperty() && level == -1) {
          propertyOK = true;
        } else {
          errorReporter.report(CoreErrorWrapper.make(new CertainTypecheckingError(CertainTypecheckingError.Kind.USELESS_LEVEL, null), field.getTypeLevel()));
        }
      }

      if (!propertyOK && !sort.isProp()) {
        errorReporter.report(new TypecheckingError("The level of property '" + field.getName() + "' must be \\Prop", null));
        return false;
      }

      // TODO[double_check]: Check covariance
    }

    // TODO[double_check]: Check occurrences of fields in other fields
    // TODO[double_check]: Check implemented
    // TODO[double_check]: Check overridden

    return true;
  }
}
