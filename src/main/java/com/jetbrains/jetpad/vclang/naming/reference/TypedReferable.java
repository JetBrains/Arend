package com.jetbrains.jetpad.vclang.naming.reference;

import javax.annotation.Nullable;
import java.util.List;

public interface TypedReferable extends Referable {
  default @Nullable ClassReferable getTypeClassReference() {
    return null;
  }

  default @Nullable Object getTypeOf() {
    return null;
  }

  default @Nullable Object getParameterType(List<Boolean> parameters) {
    return null;
  }
}
