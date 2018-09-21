package org.arend.naming.error;

import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;

public class WrongReferable extends NamingError {
  public final Referable referable;

  public WrongReferable(String message, Referable referable, Concrete.SourceNode cause) {
    super(message, cause);
    this.referable = referable;
  }
}
