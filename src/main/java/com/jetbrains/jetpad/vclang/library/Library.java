package com.jetbrains.jetpad.vclang.library;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

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
   * Gets the underling typechecker state of this library.
   *
   * @return the typechecker state.
   */
  @Nonnull
  TypecheckerState getTypecheckerState();

  /**
   * Gets the list of loaded modules of this library.
   *
   * @return the list of loaded modules.
   */
  @Nonnull
  Collection<? extends ModulePath> getLoadedModules();

  /**
   * Gets the group of a module.
   *
   * @param modulePath  the path to a module.
   *
   * @return the group of a module or null if the module is not found.
   */
  @Nullable
  Group getModuleGroup(ModulePath modulePath);

  /**
   * Provides a module scope provider that can be used to get scopes of modules in this library.
   * This method may be invoked only after the library is successfully loaded.
   *
   * @return a scope provider for modules in this library.
   */
  @Nonnull
  ModuleScopeProvider getModuleScopeProvider();
}
