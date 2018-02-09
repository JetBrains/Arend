package com.jetbrains.jetpad.vclang.source;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.library.SourceLibrary;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.error.ModuleNotFoundError;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains all necessary information for source loading.
 */
public final class SourceLoader {
  private final SourceLibrary myLibrary;
  private final ErrorReporter myErrorReporter;
  private final Map<ModulePath, Boolean> myLoadedModules = new HashMap<>();

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
    Boolean isLoaded = myLoadedModules.get(modulePath);
    if (isLoaded != null) {
      return isLoaded;
    }

    Source cacheSource = myLibrary.getCacheSource(modulePath);
    Source rawSource = myLibrary.getRawSource(modulePath);
    boolean cacheSourceIsAvailable = cacheSource != null && cacheSource.isAvailable();
    boolean rawSourceIsAvailable = rawSource != null && rawSource.isAvailable();

    boolean ok;
    if (!cacheSourceIsAvailable && !rawSourceIsAvailable) {
      myErrorReporter.report(new ModuleNotFoundError(modulePath));
      ok = false;
    } else {
      if (cacheSourceIsAvailable && rawSourceIsAvailable && cacheSource.getTimeStamp() < rawSource.getTimeStamp()) {
        cacheSourceIsAvailable = false;
      }
      ok = cacheSourceIsAvailable && cacheSource.load(this) || rawSourceIsAvailable && rawSource.load(this);
    }

    myLoadedModules.put(modulePath, ok);
    return ok;
  }
}
