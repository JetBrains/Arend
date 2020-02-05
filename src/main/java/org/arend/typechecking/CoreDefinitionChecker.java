package org.arend.typechecking;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.param.DependentLink;
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
import org.arend.typechecking.error.local.CoreErrorWrapper;
import org.arend.typechecking.error.local.inference.ArgInferenceError;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.arend.typechecking.visitor.BaseDefinitionTypechecker;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;

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

    Level level;
    if (definition.getResultTypeLevel() != null) {
      level = myChecker.checkLevelProof(definition.getResultTypeLevel(), definition.getResultType());
      if (level == null) {
        return false;
      }
    } else {
      level = null;
    }

    if (definition.getKind() == CoreFunctionDefinition.Kind.LEMMA && (level == null || !level.isProp())) {
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

    // TODO[double_check]: Check definition.hasUniverses()

    // TODO[double_check]: Check definition.getParametersLevels()

    // TODO[double_check]: Check termination

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
        errorReporter.report(new TypecheckingError("Constructor '" + constructor + "' belongs to '" + definition + "', but its data type is '" + constructor.getDataType() + "'", null));
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
        errorReporter.report(new TypecheckingError("The sort " + sort + " of constructor '" + constructor + "' does not fit into the sort " + definition.getSort() + " of its data type", null));
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
        errorReporter.report(new TypecheckingError("The h-level " + definition.getSort().getHLevel() + " of data type '" + definition + "' does not fit into the h-level " + hLevel + " of the squashing function", null));
        return false;
      }
    }

    // TODO[double_check]: Check definition.hasUniverses()

    // TODO[double_check]: Check definition.getParametersLevels()

    // TODO[double_check]: Check strict positivity

    return true;
  }

  private boolean check(ClassDefinition definition) {

    // TODO[double_check]: Check definition.hasUniverses()

    return true;
  }
}
