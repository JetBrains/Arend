package org.arend.typechecking.visitor;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.Constructor;
import org.arend.core.definition.DataDefinition;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.TypecheckingError;
import org.arend.term.FunctionKind;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.covariance.ParametersCovarianceChecker;
import org.arend.typechecking.error.local.CertainTypecheckingError;

import java.util.Set;

public class BaseDefinitionTypechecker {
  protected ErrorReporter errorReporter;

  protected BaseDefinitionTypechecker(ErrorReporter errorReporter) {
    this.errorReporter = errorReporter;
  }

  protected void checkFunctionLevel(Concrete.BaseFunctionDefinition def, FunctionKind kind) {
    if (def.getResultTypeLevel() != null && !(kind == FunctionKind.LEMMA || kind == FunctionKind.COCLAUSE_FUNC || def.getBody() instanceof Concrete.ElimFunctionBody || def.getBody().getTerm() instanceof Concrete.CaseExpression)) {
      errorReporter.report(new CertainTypecheckingError(CertainTypecheckingError.Kind.LEVEL_IGNORED, def.getResultTypeLevel()));
      def.setResultTypeLevel(null);
    }
  }

  protected boolean checkElimBody(Concrete.BaseFunctionDefinition def) {
    if (def.isRecursive() && !(def.getBody() instanceof Concrete.ElimFunctionBody)) {
      errorReporter.report(new TypecheckingError("Recursive functions must be defined by pattern matching", def));
      return false;
    } else {
      return true;
    }
  }

  public static int checkNumberInPattern(int n, ErrorReporter errorReporter, Concrete.SourceNode sourceNode) {
    if (n < 0) {
      n = -n;
    }
    if (n > Concrete.NumberPattern.MAX_VALUE) {
      n = Concrete.NumberPattern.MAX_VALUE;
    }
    if (n == Concrete.NumberPattern.MAX_VALUE) {
      errorReporter.report(new TypecheckingError("Value too big", sourceNode));
    }
    return n;
  }

  protected void getCovariantParameters(DataDefinition dataDefinition, Set<DependentLink> parameters) {
    if (parameters.isEmpty()) {
      return;
    }

    ParametersCovarianceChecker checker = new ParametersCovarianceChecker(parameters);
    for (Constructor constructor : dataDefinition.getConstructors()) {
      if (!constructor.status().headerIsOK()) {
        continue;
      }
      for (DependentLink link1 = constructor.getParameters(); link1.hasNext(); link1 = link1.getNext()) {
        link1 = link1.getNextTyped(null);
        checker.check(link1.getTypeExpr());
        if (parameters.isEmpty()) {
          return;
        }
      }
      if (checker.checkNonCovariant(constructor.getBody())) {
        return;
      }
    }
  }
}
