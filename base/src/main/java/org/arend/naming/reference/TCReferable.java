package org.arend.naming.reference;

import org.arend.ext.reference.DataContainer;
import org.arend.ext.reference.Precedence;
import org.arend.module.ModuleLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
