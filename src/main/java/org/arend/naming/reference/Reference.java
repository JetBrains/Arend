package org.arend.naming.reference;

import org.arend.ext.reference.DataContainer;
import org.jetbrains.annotations.NotNull;

public interface Reference extends DataContainer {
  @NotNull Referable getReferent();
}
