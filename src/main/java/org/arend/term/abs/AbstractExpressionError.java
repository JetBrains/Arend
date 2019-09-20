package org.arend.term.abs;

import org.arend.typechecking.error.local.LocalError;

import javax.annotation.Nonnull;

public class AbstractExpressionError extends LocalError {
  private final Object myCause;

  public AbstractExpressionError(@Nonnull Level level, String message, Object cause) {
    super(level, message);
    myCause = cause;
  }

  public static AbstractExpressionError incomplete(Object cause) {
    return new IncompleteExpressionError(cause);
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

  @Override
  public Stage getStage() {
    return Stage.PARSER;
  }
}
