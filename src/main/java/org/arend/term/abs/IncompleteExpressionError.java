package org.arend.term.abs;

public class IncompleteExpressionError extends AbstractExpressionError {
  public IncompleteExpressionError(Object cause) {
    super(Level.ERROR, "Incomplete expression", cause);
  }
}
