package org.arend.ext.concrete.level;

import org.jetbrains.annotations.NotNull;

public interface ConcreteSucLevel extends ConcreteLevel {
  @NotNull ConcreteLevel getExpression();
}
