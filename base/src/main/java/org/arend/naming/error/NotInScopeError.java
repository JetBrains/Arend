package org.arend.naming.error;

import org.arend.ext.error.LocalError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.naming.reference.Referable;
import org.jetbrains.annotations.NotNull;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

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

  @NotNull
  @Override
  public Stage getStage() {
    return Stage.RESOLVER;
  }
}
