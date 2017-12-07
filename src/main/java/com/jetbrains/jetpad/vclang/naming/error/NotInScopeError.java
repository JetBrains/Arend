package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalError;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class NotInScopeError extends LocalError {
  private final Object myCause;
  public final String name;
  public final Referable referable;

  public NotInScopeError(Object cause, Referable referable, String name) {
    super(Level.ERROR, "Cannot resolve reference");
    myCause = cause;
    this.name = name;
    this.referable = referable;
  }

  @Override
  public Object getCause() {
    return myCause;
  }

  @Override
  public LineDoc getHeaderDoc(PrettyPrinterConfig src) {
    LineDoc header = hList(super.getHeaderDoc(src), text(" '" + name + "'"));
    return referable == null ? header : hList(header, text(" in "), refDoc(referable));
  }
}
