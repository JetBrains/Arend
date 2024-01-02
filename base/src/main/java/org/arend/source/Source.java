package org.arend.source;

import org.arend.ext.module.ModulePath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Represents a persisted module.
 */
public interface Source {
  enum LoadResult { SUCCESS, FAIL, CONTINUE }

  /**
   * Gets the path to this source.
   *
   * @return path to the source.
   */
  @Nullable
  ModulePath getModulePath();

  /**
   * Runs one pass of the loading process.
   *
   * @param sourceLoader    the state of the loading process.
   *
   * @return {@link LoadResult#CONTINUE} if this method must be called again,
   *         otherwise returns either {@link LoadResult#SUCCESS} or {@link LoadResult#FAIL} depending on the result.
   */
  @NotNull LoadResult load(SourceLoader sourceLoader);

  /**
   * Gets the timestamp for this source.
   *
   * @return timestamp
   * @see File#lastModified
   */
  long getTimeStamp();

  /**
   * Checks if the source is available for loading.
   *
   * @return true if the source can be loaded and/or persisted, false otherwise.
   */
  boolean isAvailable();

  /**
   * Returns the list of modules on which this source depends.
   */
  default @NotNull List<? extends ModulePath> getDependencies() {
    return Collections.emptyList();
  }
}
