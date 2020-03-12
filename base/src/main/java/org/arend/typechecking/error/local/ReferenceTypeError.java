package org.arend.typechecking.error.local;

import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.naming.error.ReferenceError;
import org.arend.naming.reference.Referable;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class ReferenceTypeError extends ReferenceError {
  public ReferenceTypeError(Referable referable) {
    super(Stage.TYPECHECKER, "Cannot infer type of ", referable);
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig src) {
    return hList(text(message), refDoc(referable));
  }
}
