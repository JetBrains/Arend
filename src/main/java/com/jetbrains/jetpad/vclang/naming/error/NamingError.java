package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

public class NamingError<T> extends GeneralError<T> {
  public final Concrete.SourceNode<T> cause;

  public NamingError(String message, Concrete.SourceNode<T> cause) {
    super(Level.ERROR, message);
    this.cause = cause;
  }

  public NamingError(Level level, String message, Concrete.SourceNode<T> cause) {
    super(level, message);
    this.cause = cause;
  }

  @Override
  public T getCause() {
    return cause.getData();
  }

  @Override
  public Doc getCauseDoc(PrettyPrinterInfoProvider src) {
    return DocFactory.ppDoc(cause, src);
  }
}
