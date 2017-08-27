package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;

import javax.annotation.Nonnull;

public class ReferableError<T> extends GeneralError<T> {
  public final Referable referable;

  public ReferableError(@Nonnull Level level, String message, Referable referable) {
    super(level, message);
    this.referable = referable;
  }

  @Override
  public T getCause() {
    return (T) referable;
  }
}
