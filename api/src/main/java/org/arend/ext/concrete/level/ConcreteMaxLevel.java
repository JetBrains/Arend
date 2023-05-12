package org.arend.ext.concrete.level;

import org.jetbrains.annotations.NotNull;

public interface ConcreteMaxLevel extends ConcreteLevel {
  @NotNull ConcreteLevel getLeft();
  @NotNull ConcreteLevel getRight();
}
