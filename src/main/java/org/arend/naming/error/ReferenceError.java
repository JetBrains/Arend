package org.arend.naming.error;

import org.arend.error.doc.Doc;
import org.arend.error.doc.DocFactory;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.UnresolvedReference;
import org.arend.term.prettyprint.PrettyPrinterConfig;
import org.arend.typechecking.error.local.LocalError;

public class ReferenceError extends LocalError {
  public final Referable referable;

  public ReferenceError(String message, Referable referable) {
    this(Level.ERROR, message, referable);
  }

  public ReferenceError(Level level, String message, Referable referable) {
    super(level, message);
    this.referable = referable;
    if (referable instanceof GlobalReferable) {
      definition = (GlobalReferable) referable;
    }
  }

  @Override
  public Object getCause() {
    if (referable instanceof UnresolvedReference) {
      Object data = ((UnresolvedReference) referable).getData();
      if (data != null) {
        return data;
      }
    }
    return referable;
  }

  @Override
  public Doc getCauseDoc(PrettyPrinterConfig src) {
    return DocFactory.refDoc(referable);
  }
}
