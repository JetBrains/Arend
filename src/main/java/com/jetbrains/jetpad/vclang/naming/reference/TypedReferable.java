package com.jetbrains.jetpad.vclang.naming.reference;

import javax.annotation.Nullable;

public interface TypedReferable extends Referable {
  default @Nullable ClassReferable getTypeClassReference() {
    return null;
  }

  default @Nullable Object getTypeOf() {
    return null;
  }

  default @Nullable Object getParameterType(int index) {
    return null;
  }
}
