package com.jetbrains.jetpad.vclang.library;

import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;

import javax.annotation.Nonnull;

/**
 * Represents a library which can be loaded from some external source such as a file system.
 * The scopes of loaded modules can be obtained from {@link #getModuleScopeProvider}.
 */
public interface Library {
  /**
   * Gets the name of the library.
   *
   * @return the name of this library.
   */
  @Nonnull
  String getName();

  /**
   * Loads the library and its dependencies.
   * This method must register all of the library's dependencies using {@link LibraryManager#registerDependency}
   * Do not invoke this method directly; use {@link LibraryManager#loadLibrary(Library)} instead.
   *
   * @param libraryManager  a library manager containing the information necessary for the loading.
   *
   * @return true if loading succeeded, false otherwise.
   */
  boolean load(LibraryManager libraryManager);

  /**
   * Unloads the library.
   * Do not invoke this method directly; use {@link LibraryManager#unloadLibrary(Library)} instead.
   */
  void unload();

  /**
   * Checks if the library is loaded.
   *
   * @return true if the library is loaded, false otherwise.
   */
  boolean isLoaded();

  /**
   * Provides a module scope provider that can be used to get scopes of modules in this library.
   * This method may be invoked only after the library is successfully loaded.
   *
   * @return a scope provider for modules in this library.
   */
  @Nonnull
  ModuleScopeProvider getModuleScopeProvider();
}
