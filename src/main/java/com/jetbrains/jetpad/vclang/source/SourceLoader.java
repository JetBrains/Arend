package com.jetbrains.jetpad.vclang.source;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.library.LibraryManager;
import com.jetbrains.jetpad.vclang.library.SourceLibrary;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.error.ModuleNotFoundError;
import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;

import java.util.HashSet;
import java.util.Set;

/**
 * Contains all necessary information for source loading.
 */
public final class SourceLoader {
  private final SourceLibrary myLibrary;
  private final LibraryManager myLibraryManager;
  private final Set<ModulePath> myLoadedModules = new HashSet<>();
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
    if (!myLoadedModules.add(modulePath)) {
      return true;
    }

    Source binarySource = myLibrary.getBinarySource(modulePath);
    Source rawSource = myLibrary.getRawSource(modulePath);
    boolean binarySourceIsAvailable = binarySource != null && binarySource.isAvailable();
    boolean rawSourceIsAvailable = rawSource != null && rawSource.isAvailable();

    boolean result;
    if (!binarySourceIsAvailable && !rawSourceIsAvailable) {
      getErrorReporter().report(new ModuleNotFoundError(modulePath));
      result = false;
    } else {
      if (binarySourceIsAvailable && rawSourceIsAvailable && (myRecompile || binarySource.getTimeStamp() < rawSource.getTimeStamp())) {
        binarySourceIsAvailable = false;
      }
      result = binarySourceIsAvailable ? binarySource.load(this) : !binaryOnly && rawSource.load(this);
    }

    if (!result) {
      myLoadedModules.remove(modulePath);
    }
    return result;
  }

  public boolean load(ModulePath modulePath) {
    return load(modulePath, false);
  }

  public boolean loadBinary(ModulePath modulePath) {
    return load(modulePath, true);
  }
}
