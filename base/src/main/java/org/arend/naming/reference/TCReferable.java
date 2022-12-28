package org.arend.naming.reference;

import org.arend.ext.reference.DataContainer;
import org.jetbrains.annotations.NotNull;

public interface TCReferable extends LocatedReferable, DataContainer {
  @Override
  default @NotNull TCReferable getTypecheckable() {
    return this;
  }

  boolean isTypechecked();

  default boolean isLocalFunction() {
    return false;
  }
}
