package org.arend.term.abs;

import org.arend.ext.error.LocalError;

import javax.annotation.Nonnull;

public class AbstractExpressionError extends LocalError {
  private final Object myCause;

  public AbstractExpressionError(@Nonnull Level level, String message, Object cause) {
    super(level, message);
    myCause = cause;
  }

  @Override
  public Object getCause() {
    return myCause;
  }

  @Override
  public Stage getStage() {
    return Stage.PARSER;
  }
}
