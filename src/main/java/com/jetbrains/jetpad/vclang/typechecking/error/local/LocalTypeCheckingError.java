package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

import javax.annotation.Nonnull;

public class LocalTypeCheckingError extends Error {
  public final Concrete.SourceNode cause;

  public LocalTypeCheckingError(@Nonnull Level level, String message, Concrete.SourceNode cause) {
    super(level, message);
    this.cause = cause;
  }

  public LocalTypeCheckingError(String message, Concrete.SourceNode cause) {
    super(Level.ERROR, message);
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
