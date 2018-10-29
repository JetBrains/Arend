package org.arend.typechecking.error.local;

import org.arend.term.concrete.Concrete;

public class ConstructorReferenceError extends TypecheckingError {
  public ConstructorReferenceError(Concrete.SourceNode cause) {
    super("Constructors may refer only to previously defined constructors", cause);
  }
}
