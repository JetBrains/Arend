package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class IncorrectReferenceError extends TypecheckingError {
  public final Referable referable;

  public IncorrectReferenceError(Referable referable, Concrete.SourceNode sourceNode) {
    super("", sourceNode);
    this.referable = referable;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig src) {
    return hList(text("'"), refDoc(referable), text("' is not a reference to either a definition or a variable"));
  }
}
