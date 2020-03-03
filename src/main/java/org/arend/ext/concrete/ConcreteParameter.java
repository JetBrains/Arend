package org.arend.ext.concrete;

import org.jetbrains.annotations.NotNull;

public interface ConcreteParameter extends ConcreteSourceNode {
  @NotNull ConcreteParameter implicit();
}
