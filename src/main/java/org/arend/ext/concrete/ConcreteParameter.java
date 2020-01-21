package org.arend.ext.concrete;

import javax.annotation.Nonnull;

public interface ConcreteParameter extends ConcreteSourceNode {
  @Nonnull ConcreteParameter implicit();
}
