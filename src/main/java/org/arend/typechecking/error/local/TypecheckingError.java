package org.arend.typechecking.error.local;

import org.arend.error.doc.Doc;
import org.arend.error.doc.DocFactory;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import javax.annotation.Nonnull;

public class TypecheckingError extends LocalError {
  public enum Kind {
    LEVEL_IN_FUNCTION("\\level is allowed only for lemmas and functions defined by pattern matching");

    private final String message;

    Kind(String message) {
      this.message = message;
    }
  }

  public final Concrete.SourceNode cause;
  public final Kind kind;

  public TypecheckingError(@Nonnull Level level, String message, @Nonnull Concrete.SourceNode cause) {
    super(level, message);
    this.cause = cause;
    this.kind = null;
  }

  public TypecheckingError(String message, @Nonnull Concrete.SourceNode cause) {
    super(Level.ERROR, message);
    this.cause = cause;
    this.kind = null;
  }

  public TypecheckingError(Kind kind, @Nonnull Concrete.SourceNode cause) {
    super(Level.ERROR, kind.message);
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
