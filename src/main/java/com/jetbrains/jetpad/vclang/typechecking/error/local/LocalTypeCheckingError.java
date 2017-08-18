package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintable;

import javax.annotation.Nonnull;

public class LocalTypeCheckingError<T> extends Error<T> {
  public final Concrete.SourceNode<T> cause;

  public LocalTypeCheckingError(@Nonnull Level level, String message, Concrete.SourceNode<T> cause) {
    super(level, message);
    this.cause = cause;
  }

  public LocalTypeCheckingError(String message, Concrete.SourceNode<T> cause) {
    super(Level.ERROR, message);
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
