package org.arend.ext.reference;

import org.jetbrains.annotations.Nullable;

/**
 * A general interface for objects containing some unspecified data.
 */
public interface DataContainer {
  @Nullable Object getData();
}
