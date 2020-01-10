package org.arend.naming.reference;

import org.arend.ext.concrete.ArendRef;

import javax.annotation.Nonnull;

public interface Referable extends ArendRef {
  @Nonnull String textRepresentation();

  @Nonnull
  @Override
  default String getRefName() {
    return textRepresentation();
  }

  @Nonnull
  default Referable getUnderlyingReferable() {
    return this;
  }
}
