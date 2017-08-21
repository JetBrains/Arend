package com.jetbrains.jetpad.vclang.naming.reference;

import javax.annotation.Nullable;

public interface Referable {
  @Nullable
  default String getName() {
    return toString();
  }
}
