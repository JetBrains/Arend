package org.arend.naming.error;

import org.arend.term.concrete.Concrete;
import org.arend.ext.error.LocalError;

public class NamingError extends LocalError {
  public final Object cause;

  public NamingError(String message, Object cause) {
    super(Level.ERROR, message);
    this.cause = cause;
  }

  public NamingError(Level level, String message, Object cause) {
    super(level, message);
    this.cause = cause;
  }

  @Override
  public Concrete.SourceNode getCauseSourceNode() {
    return cause instanceof Concrete.SourceNode ? (Concrete.SourceNode) cause : null;
  }

  @Override
  public Object getCause() {
    return cause instanceof Concrete.SourceNode ? ((Concrete.SourceNode) cause).getData() : cause;
  }

  @Override
  public Stage getStage() {
    return Stage.RESOLVER;
  }
}
