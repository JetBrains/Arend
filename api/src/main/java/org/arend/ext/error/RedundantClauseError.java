package org.arend.ext.error;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.jetbrains.annotations.Nullable;

public class RedundantClauseError extends TypecheckingError {
  public RedundantClauseError(@Nullable ConcreteSourceNode cause) {
    super(Level.WARNING_UNUSED, "Clause is redundant", cause);
  }
}
