package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintable;

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
  public PrettyPrintable getCausePP() {
    return cause;
  }
}
