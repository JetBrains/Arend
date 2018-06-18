package com.jetbrains.jetpad.vclang.naming.reference;

import javax.annotation.Nonnull;

public interface Reference extends DataContainer {
  @Nonnull Referable getReferent();
}
