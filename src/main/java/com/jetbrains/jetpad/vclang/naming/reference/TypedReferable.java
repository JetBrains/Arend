package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.term.abs.Abstract;

import javax.annotation.Nullable;
import java.util.List;

public interface TypedReferable extends Referable {
  default @Nullable ClassReferable getTypeClassReference() {
    return null;
  }

  default @Nullable Abstract.Expression getTypeOf() {
    return null;
  }

  default @Nullable Object getParameterType(List<Boolean> parameters) {
    return null;
  }
}
