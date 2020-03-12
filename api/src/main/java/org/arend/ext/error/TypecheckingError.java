package org.arend.ext.error;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TypecheckingError extends LocalError {
  public final ConcreteSourceNode cause;

  public TypecheckingError(@NotNull Level level, @NotNull String message, @Nullable ConcreteSourceNode cause) {
    super(level, message);
    this.cause = cause;
  }

  public TypecheckingError(@NotNull String message, @Nullable ConcreteSourceNode cause) {
    super(Level.ERROR, message);
    this.cause = cause;
  }

  @Override
  public ConcreteSourceNode getCauseSourceNode() {
    return cause;
  }

  @NotNull
  @Override
  public Stage getStage() {
    return Stage.TYPECHECKER;
  }
}
