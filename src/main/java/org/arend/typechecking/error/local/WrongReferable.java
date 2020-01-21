package org.arend.typechecking.error.local;

import org.arend.ext.error.TypecheckingError;
import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;

public class WrongReferable extends TypecheckingError {
  public final Referable referable;

  public WrongReferable(String message, Referable referable, Concrete.SourceNode cause) {
    super(message, cause);
    this.referable = referable;
  }
}
