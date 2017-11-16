package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

public class NamespaceDuplicateNameError extends ReferenceError {
  public final Referable referable;
  public final Referable previous;

  public NamespaceDuplicateNameError(Level level, Referable referable, Referable previous) {
    super(level, "Duplicate name: " + referable.textRepresentation(), referable);
    this.referable = referable;
    this.previous = previous;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig src) {
    return DuplicateNameError.getBodyDoc(referable, previous);
  }
}
