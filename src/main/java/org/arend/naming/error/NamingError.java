package org.arend.naming.error;

import org.arend.error.doc.Doc;
import org.arend.error.doc.DocFactory;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrintable;
import org.arend.term.prettyprint.PrettyPrinterConfig;
import org.arend.typechecking.error.local.LocalError;

public class NamingError extends LocalError {
  public final Object cause;

  public NamingError(String message, Object cause) {
    super(Level.ERROR, message);
    this.cause = cause;
  }

  public NamingError(Level level, String message, Object cause) {
    super(level, message);
    this.cause = cause;
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
