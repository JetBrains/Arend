package org.arend.naming.error;

import org.arend.error.doc.LineDoc;
import org.arend.naming.reference.Referable;
import org.arend.term.prettyprint.PrettyPrinterConfig;
import org.arend.typechecking.error.local.LocalError;

import static org.arend.error.doc.DocFactory.*;

public class NotInScopeError extends LocalError {
  private final Object myCause;
  public final String name;
  public final Referable referable;
  public final int index;

  public NotInScopeError(Object cause, Referable referable, int index, String name) {
    super(Level.ERROR, "Cannot resolve reference");
    myCause = cause;
    this.name = name;
    this.referable = referable;
    this.index = index;
  }

  @Override
  public Object getCause() {
    return myCause;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig src) {
    LineDoc header = text(message + " '" + name + "'");
    return referable == null ? header : hList(header, text(" in "), refDoc(referable));
  }
}
