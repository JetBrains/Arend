package org.arend.naming.reference;

import org.arend.ext.reference.ArendRef;
import org.arend.ext.reference.RawRef;

import javax.annotation.Nonnull;

public interface Referable extends ArendRef, RawRef {
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
