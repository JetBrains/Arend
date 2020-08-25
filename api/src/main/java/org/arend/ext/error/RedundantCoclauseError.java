package org.arend.ext.error;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.jetbrains.annotations.Nullable;

public class RedundantCoclauseError extends TypecheckingError {
  public RedundantCoclauseError(@Nullable ConcreteSourceNode cause) {
    super(Level.WARNING_UNUSED, "Coclause is redundant", cause);
  }
}
