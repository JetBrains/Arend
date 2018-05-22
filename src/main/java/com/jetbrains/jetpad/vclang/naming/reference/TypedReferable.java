package com.jetbrains.jetpad.vclang.naming.reference;

import javax.annotation.Nullable;

public interface TypedReferable extends Referable {
  default @Nullable ClassReferable getTypeClassReference() {
    return null;
  }
}
