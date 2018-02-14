package com.jetbrains.jetpad.vclang.source;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.library.LibraryManager;
import com.jetbrains.jetpad.vclang.library.SourceLibrary;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.error.ModuleNotFoundError;
import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains all necessary information for source loading.
 */
public final class SourceLoader {
  private final SourceLibrary myLibrary;
  private final LibraryManager myLibraryManager;
  private final Map<ModulePath, Boolean> myLoadedModules = new HashMap<>();

  public SourceLoader(SourceLibrary library, LibraryManager libraryManager) {
    myLibrary = library;
    myLibraryManager = libraryManager;
  }

  public SourceLibrary getLibrary() {
    return myLibrary;
  }

  public ModuleScopeProvider getModuleScopeProvider() {
    return myLibraryManager.getModuleScopeProvider();
  }

  public ErrorReporter getErrorReporter() {
    return myLibraryManager.getErrorReporter();
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
      getErrorReporter().report(new ModuleNotFoundError(modulePath));
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
