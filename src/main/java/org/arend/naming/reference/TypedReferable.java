package org.arend.naming.reference;

import org.arend.term.abs.Abstract;
import org.jetbrains.annotations.Nullable;

public interface TypedReferable extends Referable {
  default @Nullable ClassReferable getTypeClassReference() {
    return null;
  }

  default @Nullable Abstract.Expression getTypeOf() {
    return null;
  }
}
