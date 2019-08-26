package org.arend.typechecking.error.local;

import org.arend.error.doc.Doc;
import org.arend.error.doc.DocFactory;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import javax.annotation.Nonnull;

public class DesugaringError extends LocalError {
  public enum Kind {
    REDUNDANT_COCLAUSE(Level.WEAK_WARNING, "Coclause is redundant"),
    EXPECTED_EXPLICIT(Level.ERROR, "Expected an explicit argument");

    private final Level level;
    private final String message;

    Kind(Level level, String message) {
      this.level = level;
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

  public DesugaringError(Kind kind, @Nonnull Concrete.SourceNode cause) {
    super(kind.level, kind.message);
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

  @Override
  public boolean isTypecheckingError() {
    return true;
  }
}
