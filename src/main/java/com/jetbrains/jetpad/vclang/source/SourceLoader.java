package com.jetbrains.jetpad.vclang.source;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.library.SourceLibrary;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.error.ModuleNotFoundError;

/**
 * Contains all necessary information for source loading.
 */
public final class SourceLoader {
  private final SourceLibrary myLibrary;
  private final ErrorReporter myErrorReporter;

  public SourceLoader(SourceLibrary library, ErrorReporter errorReporter) {
    myLibrary = library;
    myErrorReporter = errorReporter;
  }

  public SourceLibrary getLibrary() {
    return myLibrary;
  }

  public ErrorReporter getErrorReporter() {
    return myErrorReporter;
  }

  public boolean load(ModulePath modulePath) {
    if (myLibrary.isModuleRegistered(modulePath)) {
      return true;
    }

    Source cacheSource = myLibrary.getCacheSource(modulePath);
    Source rawSource = myLibrary.getRawSource(modulePath);
    boolean cacheSourceIsAvailable = cacheSource != null && cacheSource.isAvailable();
    boolean rawSourceIsAvailable = rawSource != null && rawSource.isAvailable();

    if (!cacheSourceIsAvailable && !rawSourceIsAvailable) {
      myErrorReporter.report(new ModuleNotFoundError(modulePath));
      return false;
    }

    if (cacheSourceIsAvailable && rawSourceIsAvailable && cacheSource.getTimeStamp() < rawSource.getTimeStamp()) {
      cacheSourceIsAvailable = false;
    }

    return cacheSourceIsAvailable && cacheSource.load(this) || rawSourceIsAvailable && rawSource.load(this);
  }
}
