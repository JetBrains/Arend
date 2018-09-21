package org.arend.library.resolver;

import org.arend.library.Library;

import javax.annotation.Nullable;

/**
 * Resolves library's name.
 */
public interface LibraryResolver {
  /**
   * Resolves library's name.
   *
   * @param name  library's name.
   *
   * @return corresponding library or null if the library cannot be resolved.
   */
  @Nullable
  Library resolve(String name);
}
