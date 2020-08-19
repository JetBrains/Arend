package org.arend.ext.error;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.jetbrains.annotations.Nullable;

public class IgnoredArgumentError extends TypecheckingError {
  public IgnoredArgumentError(@Nullable ConcreteSourceNode cause) {
    super(Level.WARNING_UNUSED, "Argument is ignored", cause);
  }
}
