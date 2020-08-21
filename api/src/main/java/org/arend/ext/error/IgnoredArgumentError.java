package org.arend.ext.error;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IgnoredArgumentError extends TypecheckingError {
  public IgnoredArgumentError(@NotNull String message, @Nullable ConcreteSourceNode cause) {
    super(Level.WARNING_UNUSED, message, cause);
  }

  public IgnoredArgumentError(@Nullable ConcreteSourceNode cause) {
    this("Argument is ignored", cause);
  }
}
