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
  private final Map<ModulePath, SourceType> myLoadedModules = new HashMap<>();
  private final Map<ModulePath, BinarySource> myLoadingBinaryModules = new HashMap<>();
  private final boolean myRecompile;

  private enum SourceType { RAW, BINARY }

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

  public ErrorReporter getTypecheckingErrorReporter() {
    return myLibraryManager.getTypecheckingErrorReporter();
  }

  public ErrorReporter getLibraryErrorReporter() {
    return myLibraryManager.getLibraryErrorReporter();
  }

  /**
   * Loads either raw or binary source.
   *
   * @param modulePath  a module to load.
   * @return true if the source was successfully loaded, false otherwise.
   */
  public boolean load(ModulePath modulePath) {
    if (myLoadedModules.containsKey(modulePath)) {
      return true;
    }

    BinarySource binarySource = myLibrary.getBinarySource(modulePath);
    Source rawSource = myLibrary.getRawSource(modulePath);
    boolean binarySourceIsAvailable = binarySource != null && binarySource.isAvailable();
    boolean rawSourceIsAvailable = rawSource != null && rawSource.isAvailable();

    if (!binarySourceIsAvailable && !rawSourceIsAvailable) {
      getLibraryErrorReporter().report(new ModuleNotFoundError(modulePath));
      return false;
    }

    if (binarySourceIsAvailable && rawSourceIsAvailable && (myRecompile || binarySource.getTimeStamp() < rawSource.getTimeStamp())) {
      binarySourceIsAvailable = false;
    }

    if (binarySourceIsAvailable) {
      boolean tryRaw = rawSourceIsAvailable && myLibrary.supportsMixedSources();
      boolean rawIsLoaded = tryRaw && rawSource.load(this);

      myLoadingBinaryModules.put(modulePath, binarySource);
      if (binarySource.loadDependencyInfo(this)) {
        return loadBinary(modulePath);
      }
      myLoadingBinaryModules.remove(modulePath);

      if (tryRaw) {
        myLoadedModules.put(modulePath, SourceType.RAW);
        return rawIsLoaded;
      }
    }

    if (rawSourceIsAvailable) {
      myLoadedModules.put(modulePath, SourceType.RAW);
      return rawSource.load(this);
    } else {
      return false;
    }
  }

  /**
   * Loads a raw source.
   * If a binary source is available, does not load anything and returns true immediately.
   *
   * @param modulePath  a module to load.
   * @return true if a binary source is available or if the raw source was successfully loaded, false otherwise.
   */
  public boolean loadRaw(ModulePath modulePath) {
    if (myLoadedModules.containsKey(modulePath)) {
      return true;
    }

    Source binarySource = myLibrary.getBinarySource(modulePath);
    Source rawSource = myLibrary.getRawSource(modulePath);
    boolean binarySourceIsAvailable = binarySource != null && binarySource.isAvailable();
    boolean rawSourceIsAvailable = rawSource != null && rawSource.isAvailable();

    if (binarySourceIsAvailable && rawSourceIsAvailable && (myRecompile || binarySource.getTimeStamp() < rawSource.getTimeStamp())) {
      binarySourceIsAvailable = false;
    }
    if (binarySourceIsAvailable) {
      return true;
    }

    if (!rawSourceIsAvailable) {
      getLibraryErrorReporter().report(new ModuleNotFoundError(modulePath));
      return false;
    }

    myLoadedModules.put(modulePath, SourceType.RAW);
    return rawSource.load(this);
  }

  /**
   * Loads a binary source.
   * This method can be invoked only after {@link #loadBinaryDependencyInfo}.
   *
   * @param modulePath  a module to load.
   * @return true if the source was successfully loaded, false otherwise.
   */
  public boolean loadBinary(ModulePath modulePath) {
    BinarySource binarySource = myLoadingBinaryModules.remove(modulePath);
    if (binarySource == null) {
      return true;
    }

    myLoadedModules.put(modulePath, SourceType.BINARY);
    return binarySource.load(this);
  }

  /**
   * Loads the dependency info of a binary source.
   *
   * @param modulePath  a module to load.
   * @return true if the source was successfully loaded, false otherwise.
   */
  public boolean loadBinaryDependencyInfo(ModulePath modulePath) {
    SourceType sourceType = myLoadedModules.get(modulePath);
    if (sourceType != null) {
      return sourceType == SourceType.BINARY;
    }
    if (myLoadingBinaryModules.containsKey(modulePath)) {
      return true;
    }

    BinarySource binarySource = myLibrary.getBinarySource(modulePath);
    if (binarySource == null || !binarySource.isAvailable()) {
      return false;
    }

    Source rawSource = myLibrary.getRawSource(modulePath);
    if (rawSource != null && rawSource.isAvailable() && (myRecompile || binarySource.getTimeStamp() < rawSource.getTimeStamp())) {
      return false;
    }

    myLoadingBinaryModules.put(modulePath, binarySource);
    if (binarySource.loadDependencyInfo(this)) {
      return true;
    } else {
      myLoadingBinaryModules.remove(modulePath);
      return false;
    }
  }
}
