package org.arend.ext.concrete.level;

import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.NotNull;

public interface ConcreteVarLevel extends ConcreteLevel {
  @NotNull ArendRef getReferent();
}
