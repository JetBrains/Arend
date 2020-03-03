package org.arend.library.resolver;

import org.arend.library.Library;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves dependencies names of a library.
 */
public interface LibraryResolver {
  /**
   * Resolves dependencies names of a library.
   *
   * @param library         a library.
   * @param dependencyName  a name of a dependency of the library.
   *
   * @return corresponding library or null if the library cannot be resolved.
   */
  @Nullable
  Library resolve(Library library, String dependencyName);
}
