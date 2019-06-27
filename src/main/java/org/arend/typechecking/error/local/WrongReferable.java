package org.arend.typechecking.error.local;

import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;

public class WrongReferable extends TypecheckingError {
  private final boolean myTypechecking;
  public final Referable referable;

  public WrongReferable(String message, Referable referable, boolean isTypechecking, Concrete.SourceNode cause) {
    super(message, cause);
    this.referable = referable;
    myTypechecking = isTypechecking;
  }

  public WrongReferable(String message, Referable referable, Concrete.SourceNode cause) {
    this(message, referable, true, cause);
  }

  @Override
  public boolean isTypecheckingError() {
    return myTypechecking;
  }
}
