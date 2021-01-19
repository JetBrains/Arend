package org.arend.ext.reference;

import org.arend.ext.module.LongName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A reference either to a definition or a local binding.
 */
public interface ArendRef {
  @NotNull String getRefName();

  /**
   * Returns the long name of a definition; returns null for local bindings.
   */
  default @Nullable LongName getRefLongName() {
    return null;
  }

  default boolean isClassField() {
    return false;
  }

  boolean isLocalRef();
}
