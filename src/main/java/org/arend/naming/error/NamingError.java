package org.arend.naming.error;

import org.arend.error.doc.Doc;
import org.arend.error.doc.DocFactory;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrintable;
import org.arend.term.prettyprint.PrettyPrinterConfig;
import org.arend.typechecking.error.local.LocalError;

public class NamingError extends LocalError {
  public enum Kind {
    USE_IN_CLASS("\\use is not allowed inside a class definition"),
    LEVEL_IN_FIELD("\\level is allowed only for properties"),
    CLASSIFYING_FIELD_IN_RECORD("Records cannot have classifying fields"),
    INVALID_PRIORITY("The priority must be between 0 and 10");

    private final String message;

    Kind(String message) {
      this.message = message;
    }
  }

  public final Object cause;
  public final Kind kind;

  public NamingError(String message, Object cause) {
    super(Level.ERROR, message);
    this.cause = cause;
    this.kind = null;
  }

  public NamingError(Level level, String message, Object cause) {
    super(level, message);
    this.cause = cause;
    this.kind = null;
  }

  public NamingError(Kind kind, Object cause) {
    super(Level.ERROR, kind.message);
    this.cause = cause;
    this.kind = kind;
  }

  @Override
  public Object getCause() {
    return cause instanceof Concrete.SourceNode ? ((Concrete.SourceNode) cause).getData() : cause;
  }

  @Override
  public Doc getCauseDoc(PrettyPrinterConfig src) {
    return cause instanceof Concrete.SourceNode ? DocFactory.ppDoc((PrettyPrintable) cause, src) :
           cause instanceof PrettyPrintable ? ((PrettyPrintable) cause).prettyPrint(src) :
           null;
  }
}
