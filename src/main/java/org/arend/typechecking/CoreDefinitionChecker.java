package org.arend.typechecking;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.DataDefinition;
import org.arend.core.definition.Definition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.elimtree.Body;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.elimtree.IntervalElim;
import org.arend.core.expr.*;
import org.arend.core.expr.type.Type;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.ext.core.definition.CoreFunctionDefinition;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.TypeMismatchError;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.arend.prelude.Prelude;
import org.arend.typechecking.error.local.CoreErrorWrapper;
import org.arend.typechecking.implicitargs.equations.DummyEquations;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;

public class CoreDefinitionChecker {
  private final CoreExpressionChecker myChecker;

  public CoreDefinitionChecker(ErrorReporter errorReporter) {
    myChecker = new CoreExpressionChecker(errorReporter, new HashSet<>(), DummyEquations.getInstance(), null);
  }

  public boolean check(Definition definition) {
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

  public boolean check(FunctionDefinition definition) {
    myChecker.clear();
    if (!myChecker.checkDependentLink(definition.getParameters(), Type.OMEGA, null)) {
      return false;
    }

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
          myChecker.getErrorReporter().report(new CoreErrorWrapper(new TypecheckingError("Cannot infer the sort of the type", null), definition.getResultType()));
          return false;
        }
        if (!sort.isProp()) {
          myChecker.getErrorReporter().report(new CoreErrorWrapper(new TypeMismatchError(new UniverseExpression(Sort.PROP), new UniverseExpression(sort), null), definition.getResultType()));
          return false;
        }
      }
    }

    // TODO[double_check]: Check definition.hasUniverses()

    // TODO[double_check]: Check definition.getParametersLevels()

    Body body = definition.getActualBody();
    if (body instanceof Expression) {
      return ((Expression) body).accept(myChecker, definition.getResultType()) != null;
    }

    if (body instanceof IntervalElim) {
      IntervalElim intervalElim = (IntervalElim) body;
      if (intervalElim.getCases().isEmpty()) {
        myChecker.getErrorReporter().report(new TypecheckingError("Empty IntervalElim", null));
        return false;
      }

      int offset = intervalElim.getOffset();
      DependentLink link = definition.getParameters();
      for (int i = 0; i < offset && link.hasNext(); i++) {
        link = link.getNext();
      }

      for (IntervalElim.CasePair casePair : intervalElim.getCases()) {
        if (!link.hasNext()) {
          myChecker.getErrorReporter().report(new TypecheckingError("Interval elim has too many parameters", null));
          return false;
        }

        DataCallExpression dataCall = link.getTypeExpr().normalize(NormalizationMode.WHNF).cast(DataCallExpression.class);
        if (!(dataCall != null && dataCall.getDefinition() == Prelude.INTERVAL)) {
          myChecker.getErrorReporter().report(new TypeMismatchError(new DataCallExpression(Prelude.INTERVAL, Sort.PROP, Collections.emptyList()), link.getTypeExpr(), null));
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
        myChecker.getErrorReporter().report(new TypeMismatchError(DocFactory.text("a classCall"), definition.getResultType(), null));
        return false;
      }
      return myChecker.checkCocoverage(classCall);
    } else {
      throw new IllegalStateException();
    }
  }

  public boolean check(DataDefinition definition) {
    myChecker.clear();
    return true;
  }

  public boolean check(ClassDefinition definition) {
    myChecker.clear();
    return true;
  }
}
