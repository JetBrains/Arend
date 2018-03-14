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
  private final boolean myRecompile;

  public SourceLoader(SourceLibrary library, LibraryManager libraryManager, boolean recompile) {
    myLibrary = library;
    myLibraryManager = libraryManager;
    myRecompile = recompile;
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

  private boolean load(ModulePath modulePath, boolean binaryOnly) {
    Boolean isLoaded = myLoadedModules.putIfAbsent(modulePath, true);
    if (isLoaded != null) {
      return isLoaded;
    }

    Source binarySource = myLibrary.getBinarySource(modulePath);
    Source rawSource = myLibrary.getRawSource(modulePath);
    boolean binarySourceIsAvailable = binarySource != null && binarySource.isAvailable();
    boolean rawSourceIsAvailable = rawSource != null && rawSource.isAvailable();

    Source.LoadingResult result;
    if (!binarySourceIsAvailable && !rawSourceIsAvailable) {
      getErrorReporter().report(new ModuleNotFoundError(modulePath));
      result = Source.LoadingResult.FAIL;
    } else {
      if (binarySourceIsAvailable && rawSourceIsAvailable && (myRecompile || binarySource.getTimeStamp() < rawSource.getTimeStamp())) {
        binarySourceIsAvailable = false;
      }
      result = binarySourceIsAvailable ? binarySource.load(this) : Source.LoadingResult.TRY_ANOTHER;
      if (!binaryOnly && result == Source.LoadingResult.TRY_ANOTHER && rawSourceIsAvailable) {
        result = rawSource.load(this);
      }
    }

    if (result != Source.LoadingResult.OK) {
      if (binaryOnly) {
        myLoadedModules.remove(modulePath);
      } else {
        myLoadedModules.put(modulePath, false);
      }
    }
    return result == Source.LoadingResult.OK;
  }

  public boolean load(ModulePath modulePath) {
    return load(modulePath, false);
  }

  public boolean loadBinary(ModulePath modulePath) {
    return load(modulePath, true);
  }
}
