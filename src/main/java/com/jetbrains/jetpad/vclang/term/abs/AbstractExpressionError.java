package com.jetbrains.jetpad.vclang.term.abs;

import com.jetbrains.jetpad.vclang.error.GeneralError;

import javax.annotation.Nonnull;

public class AbstractExpressionError extends GeneralError {
  private final Object myCause;

  AbstractExpressionError(@Nonnull Level level, String message, Object cause) {
    super(level, message);
    myCause = cause;
  }

  public static AbstractExpressionError incomplete(Object cause) {
    return new AbstractExpressionError(Level.ERROR, "Incomplete expression", cause);
  }

  @Override
  public Object getCause() {
    return myCause;
  }

  public static class Exception extends RuntimeException {
    public final AbstractExpressionError error;

    public Exception(AbstractExpressionError error) {
      super(error.message);
      this.error = error;
    }
  }
}
