package com.jetbrains.jetpad.vclang.library;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;

import javax.annotation.Nonnull;

/**
 * Represents a library which can be loaded from some external source such as a file system.
 * The scopes of loaded modules can be obtained from {@link #getModuleScopeProvider}.
 */
public interface Library {
  /**
   * Loads the library.
   * All dependencies of the library must be loaded before calling this method.
   *
   * @param errorReporter   a reporter for all errors that occur during loading process.
   *
   * @return true if loading succeeded, false otherwise.
   */
  boolean load(ErrorReporter errorReporter);

  /**
   * Unloads the library.
   * All libraries depending on this one must be unloaded before calling this method.
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
