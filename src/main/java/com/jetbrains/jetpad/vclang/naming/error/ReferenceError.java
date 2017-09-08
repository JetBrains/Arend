package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.UnresolvedReference;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

public class ReferenceError extends GeneralError {
  public final Referable referable;

  public ReferenceError(String message, Referable referable) {
    super(Level.ERROR, message);
    this.referable = referable;
  }

  public ReferenceError(Level level, String message, Referable referable) {
    super(level, message);
    this.referable = referable;
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
  public Doc getCauseDoc(PrettyPrinterInfoProvider src) {
    return DocFactory.refDoc(referable);
  }
}
