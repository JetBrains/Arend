package org.arend.typechecking.error.local;

import org.arend.error.doc.Doc;
import org.arend.error.doc.DocFactory;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import javax.annotation.Nonnull;

public class DesugaringError extends LocalError {
  public enum Kind {
    REDUNDANT_COCLAUSE("Coclause is redundant");

    private final String message;

    Kind(String message) {
      this.message = message;
    }
  }

  public final Concrete.SourceNode cause;
  public final Kind kind;

  public DesugaringError(String message, @Nonnull Concrete.SourceNode cause) {
    super(Level.ERROR, message);
    this.cause = cause;
    this.kind = null;
  }

  public DesugaringError(@Nonnull Level level, Kind kind, @Nonnull Concrete.SourceNode cause) {
    super(level, kind.message);
    this.cause = cause;
    this.kind = kind;
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
