package org.arend.ext.error;

import org.arend.ext.concrete.ConcreteSourceNode;

import javax.annotation.Nonnull;

public class TypecheckingError extends LocalError {
  public final ConcreteSourceNode cause;

  public TypecheckingError(@Nonnull Level level, String message, @Nonnull ConcreteSourceNode cause) {
    super(level, message);
    this.cause = cause;
  }

  public TypecheckingError(String message, @Nonnull ConcreteSourceNode cause) {
    super(Level.ERROR, message);
    this.cause = cause;
  }

  @Override
  public ConcreteSourceNode getCauseSourceNode() {
    return cause;
  }

  @Override
  public Stage getStage() {
    return Stage.TYPECHECKER;
  }
}
