package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.naming.error.ReferenceError;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

public class ReferenceTypeError extends ReferenceError {
  public ReferenceTypeError(Referable referable) {
    super("Cannot infer type of ", referable);
  }

  @Override
  public LineDoc getHeaderDoc(PrettyPrinterConfig src) {
    return DocFactory.hList(super.getHeaderDoc(src), DocFactory.refDoc(referable));
  }
}
