package org.arend.naming.reference;

import javax.annotation.Nonnull;

public interface Referable {
  @Nonnull String textRepresentation();

  default @Nonnull Referable getUnderlyingReferable() {
    return this;
  }
}
