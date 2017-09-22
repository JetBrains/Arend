package com.jetbrains.jetpad.vclang.term.abs;

import com.jetbrains.jetpad.vclang.error.GeneralError;

import javax.annotation.Nonnull;

public class AbstractConversionError extends GeneralError {
  private final Object myCause;

  AbstractConversionError(@Nonnull Level level, String message, Object cause) {
    super(level, message);
    myCause = cause;
  }

  @Override
  public Object getCause() {
    return myCause;
  }

  public static class Exception extends RuntimeException {
    public final AbstractConversionError error;

    Exception(AbstractConversionError error) {
      super(error.message);
      this.error = error;
    }
  }
}
