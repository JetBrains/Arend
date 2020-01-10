package org.arend.source;

import org.arend.ext.module.ModulePath;

import javax.annotation.Nullable;
import java.io.File;

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
   * Loads the structure of the source and its dependencies.
   *
   * @param sourceLoader    the state of the loading process.
   *
   * @return true if all dependencies are available, false otherwise.
   */
  boolean preload(SourceLoader sourceLoader);

  /**
   * This method is called after all dependencies of the source were preloaded.
   *
   * @param sourceLoader    the state of the loading process.
   *
   * @return {@link LoadResult#CONTINUE} if this method must be called again,
   *         otherwise returns either {@link LoadResult#SUCCESS} or {@link LoadResult#FAIL} depending on the result.
   */
  LoadResult load(SourceLoader sourceLoader);

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
}
