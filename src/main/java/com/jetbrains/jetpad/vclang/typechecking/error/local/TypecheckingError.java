package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import javax.annotation.Nonnull;

public class TypecheckingError extends LocalError {
  public final Concrete.SourceNode cause;

  public TypecheckingError(@Nonnull Level level, String message, Concrete.SourceNode cause) {
    super(level, message);
    this.cause = cause;
  }

  public TypecheckingError(String message, Concrete.SourceNode cause) {
    super(Level.ERROR, message);
    this.cause = cause;
  }

  @Override
  public Object getCause() {
    return cause.getData();
  }

  @Override
  public Doc getCauseDoc(PrettyPrinterConfig src) {
    return DocFactory.ppDoc(cause, src);
  }
}
