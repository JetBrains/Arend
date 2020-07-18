package org.arend.ext.error;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.jetbrains.annotations.NotNull;

public class NameResolverError extends LocalError {
  public final Object cause;

  public NameResolverError(String message, Object cause) {
    super(Level.ERROR, message);
    this.cause = cause;
  }

  public NameResolverError(Level level, String message, Object cause) {
    super(level, message);
    this.cause = cause;
  }

  @Override
  public ConcreteSourceNode getCauseSourceNode() {
    return cause instanceof ConcreteSourceNode ? (ConcreteSourceNode) cause : null;
  }

  @Override
  public Object getCause() {
    return cause instanceof ConcreteSourceNode ? ((ConcreteSourceNode) cause).getData() : cause;
  }

  @NotNull
  @Override
  public Stage getStage() {
    return Stage.RESOLVER;
  }
}
