package org.arend.naming.reference;

import org.jetbrains.annotations.Nullable;

public interface TypedReferable extends Referable {
  default @Nullable ClassReferable getTypeClassReference() {
    return null;
  }
}
