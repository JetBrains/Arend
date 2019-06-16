package org.arend.typechecking.error.local;

import org.arend.error.doc.LineDoc;
import org.arend.naming.error.ReferenceError;
import org.arend.naming.reference.Referable;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import static org.arend.error.doc.DocFactory.hList;
import static org.arend.error.doc.DocFactory.refDoc;
import static org.arend.error.doc.DocFactory.text;

public class ReferenceTypeError extends ReferenceError {
  public ReferenceTypeError(Referable referable) {
    super("Cannot infer type of ", referable);
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig src) {
    return hList(text(message), refDoc(referable));
  }

  @Override
  public boolean isTypecheckingError() {
    return true;
  }
}
