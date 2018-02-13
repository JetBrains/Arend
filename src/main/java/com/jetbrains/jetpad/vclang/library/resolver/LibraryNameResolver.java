package com.jetbrains.jetpad.vclang.library.resolver;

import com.jetbrains.jetpad.vclang.library.Library;

import javax.annotation.Nullable;

/**
 * Resolves library's name.
 */
public interface LibraryNameResolver {
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
