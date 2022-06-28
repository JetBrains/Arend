package org.arend.typechecking.error.local;

import org.arend.ext.error.TypecheckingError;
import org.arend.term.concrete.Concrete;

public class CannotFindConstructorError extends TypecheckingError {
  public CannotFindConstructorError(Concrete.SourceNode cause) {
    super("Cannot find a reference to constructor among provided patterns", cause);
  }
}
