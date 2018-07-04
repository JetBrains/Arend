package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.naming.error.ReferenceError;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.hList;
import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.refDoc;
import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.text;

public class ReferenceTypeError extends ReferenceError {
  public ReferenceTypeError(Referable referable) {
    super("Cannot infer type of ", referable);
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig src) {
    return hList(text(message), refDoc(referable));
  }
}
