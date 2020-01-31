package org.arend.ext.error;

import org.arend.ext.concrete.ConcreteSourceNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TypecheckingError extends LocalError {
  public final ConcreteSourceNode cause;

  public TypecheckingError(@Nonnull Level level, @Nonnull String message, @Nullable ConcreteSourceNode cause) {
    super(level, message);
    this.cause = cause;
  }

  public TypecheckingError(@Nonnull String message, @Nullable ConcreteSourceNode cause) {
    super(Level.ERROR, message);
    this.cause = cause;
  }

  @Override
  public ConcreteSourceNode getCauseSourceNode() {
    return cause;
  }

  @Nonnull
  @Override
  public Stage getStage() {
    return Stage.TYPECHECKER;
  }
}
