package org.arend.ext.serialization;

import org.jetbrains.annotations.NotNull;

public interface SerializableKeyRegistry {
  /**
   * Registers the given key.
   */
  void registerKey(@NotNull SerializableKey<?> key);

  /**
   * Registers all non-null fields of type {@link SerializableKey} defined in {@code keyContainer}.
   */
  void registerAllKeys(@NotNull Object keyContainer);
}
