package org.arend.ext.reference;

import org.arend.ext.module.LongName;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ArendRef {
  @Nonnull String getRefName();

  @Nullable
  default LongName getRefLongName() {
    return null;
  }

  default boolean isClassField() {
    return false;
  }
}
