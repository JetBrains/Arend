package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

public class NamingError extends GeneralError {
  public final Concrete.SourceNode cause;

  public NamingError(String message, Concrete.SourceNode cause) {
    super(Level.ERROR, message);
    this.cause = cause;
  }

  public NamingError(Level level, String message, Concrete.SourceNode cause) {
    super(level, message);
    this.cause = cause;
  }

  @Override
  public Object getCause() {
    return cause.getData();
  }

  @Override
  public Doc getCauseDoc(PrettyPrinterInfoProvider src) {
    return DocFactory.ppDoc(cause, src);
  }
}
