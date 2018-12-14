package org.arend.typechecking.error.local;

import org.arend.error.doc.Doc;
import org.arend.error.doc.DocFactory;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import javax.annotation.Nonnull;

public class TypecheckingError extends LocalError {
  public final Concrete.SourceNode cause;

  public TypecheckingError(@Nonnull Level level, String message, @Nonnull Concrete.SourceNode cause) {
    super(level, message);
    this.cause = cause;
  }

  public TypecheckingError(String message, @Nonnull Concrete.SourceNode cause) {
    super(Level.ERROR, message);
    this.cause = cause;
  }

  @Override
  public Object getCause() {
    return cause.getData();
  }

  @Override
  public Doc getCauseDoc(PrettyPrinterConfig src) {
    return DocFactory.ppDoc(cause, src);
  }
}
