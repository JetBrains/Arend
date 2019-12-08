package org.arend.typechecking.visitor;

import org.arend.error.ErrorReporter;
import org.arend.term.FunctionKind;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.TypecheckingError;

public class BaseDefinitionTypechecker {
  public ErrorReporter errorReporter;

  protected BaseDefinitionTypechecker(ErrorReporter errorReporter) {
    this.errorReporter = errorReporter;
  }

  protected void checkFunctionLevel(Concrete.BaseFunctionDefinition def, FunctionKind kind) {
    if (def.getResultTypeLevel() != null && !(kind == FunctionKind.LEMMA || kind == FunctionKind.COCLAUSE_FUNC || def.getBody() instanceof Concrete.ElimFunctionBody)) {
      errorReporter.report(new TypecheckingError(TypecheckingError.Kind.LEVEL_IGNORED, def.getResultTypeLevel()));
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
}
