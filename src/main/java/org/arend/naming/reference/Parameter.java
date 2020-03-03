package org.arend.naming.reference;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface Parameter {
  boolean isExplicit();
  @NotNull List<? extends Referable> getReferableList();
}

