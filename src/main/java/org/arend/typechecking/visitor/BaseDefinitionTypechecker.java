package org.arend.typechecking.visitor;

import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.LocalErrorReporter;
import org.arend.typechecking.error.local.TypecheckingError;

public class BaseDefinitionTypechecker {
  public LocalErrorReporter errorReporter;

  protected BaseDefinitionTypechecker(LocalErrorReporter errorReporter) {
    this.errorReporter = errorReporter;
  }

  protected void checkFunctionLevel(Concrete.FunctionDefinition def) {
    if (def.getResultTypeLevel() != null && !(def.getKind() == Concrete.FunctionDefinition.Kind.LEMMA || def.getBody() instanceof Concrete.ElimFunctionBody)) {
      errorReporter.report(new TypecheckingError(TypecheckingError.Kind.LEVEL_IN_FUNCTION, def.getResultTypeLevel()));
      def.setResultTypeLevel(null);
    }
  }

  protected void checkElimBody(Concrete.FunctionDefinition def) {
    if (def.isRecursive() && !(def.getBody() instanceof Concrete.ElimFunctionBody)) {
      errorReporter.report(new TypecheckingError("Recursive functions must be defined by pattern matching", def));
    }
  }
}
