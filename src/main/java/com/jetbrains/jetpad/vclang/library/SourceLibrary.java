package com.jetbrains.jetpad.vclang.library;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.source.Source;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Represents a library which can load cached modules (see {@link #getCacheSource})
 * as well as ordinary modules (see {@link #getRawSource}).
 */
public abstract class SourceLibrary implements Library {
  private final TypecheckerState myTypecheckerState;
  private Map<ModulePath, Set<GlobalReferable>> myModules;

  /**
   * Creates a new {@code SourceLibrary}
   *
   * @param typecheckerState  a typechecker state in which the result of loading of cached modules will be stored.
   */
  protected SourceLibrary(TypecheckerState typecheckerState) {
    myTypecheckerState = typecheckerState;
  }

  /**
   * Gets the raw source (that is, the source containing not typechecked data) for a given module path.
   *
   * @param modulePath  a path to the source.
   *
   * @return the raw source corresponding to the given path or null if the source is not found.
   */
  @Nullable
  abstract Source getRawSource(ModulePath modulePath);

  /**
   * Gets the cache source (that is, the source containing typechecked data) for a given module path.
   *
   * @param modulePath  a path to the source.
   *
   * @return the cache source corresponding to the given path or null if the source is not found.
   */
  @Nullable
  abstract Source getCacheSource(ModulePath modulePath);

  /**
   * Gets the list of modules in this library.
   *
   * @return the list of modules.
   */
  @Nonnull
  abstract Collection<? extends ModulePath> getModules();

  /**
   * Gets the underlying typechecker state of this library.
   *
   * @return the typechecker state.
   */
  public TypecheckerState getTypecheckerState() {
    return myTypecheckerState;
  }

  /**
   * Registers a module in this library.
   * This method is usually invoked during loading of the library.
   *
   * @param modulePath  the path to the module.
   */
  public void registerModule(ModulePath modulePath) {
    if (!isLoaded()) {
      throw new IllegalStateException("Library is not loaded");
    }

    myModules.put(modulePath, new HashSet<>());
  }

  /**
   * Unregisters a module.
   * The module must be registered before this method is invoked.
   * This method is usually invoked during unloading of the library.
   *
   * @param modulePath  the path to the module.
   */
  public void unregisterModule(ModulePath modulePath) {
    if (!isLoaded()) {
      throw new IllegalStateException("Library is not loaded");
    }

    for (GlobalReferable referable : myModules.remove(modulePath)) {
      myTypecheckerState.reset(referable);
    }
  }

  /**
   * Checks if a module was registered in this library.
   *
   * @param modulePath  the path to the module.
   *
   * @return true if the module was registered, false otherwise.
   */
  public boolean isModuleRegistered(ModulePath modulePath) {
    if (!isLoaded()) {
      throw new IllegalStateException("Library is not loaded");
    }

    return myModules.containsKey(modulePath);
  }

  /**
   * Registers a definition in the specified module.
   * The module must be registered before this method is invoked.
   * This method is usually invoked during loading of the library.
   *
   * @param modulePath  the module to which the definition belongs.
   * @param referable   the referable corresponding to the definition.
   * @param definition  the definition to be added to the module.
   */
  public void registerDefinition(ModulePath modulePath, GlobalReferable referable, Definition definition) {
    if (!isLoaded()) {
      throw new IllegalStateException("Library is not loaded");
    }

    myModules.get(modulePath).add(referable);
    myTypecheckerState.record(referable, definition);
  }

  /**
   * Unregisters a definition.
   * The module must be registered before this method is invoked.
   *
   * @param modulePath  the module to which the definition belongs.
   * @param referable   the referable corresponding to the definition.
   */
  public void unregisterDefinition(ModulePath modulePath, GlobalReferable referable) {
    if (!isLoaded()) {
      throw new IllegalStateException("Library is not loaded");
    }

    myModules.get(modulePath).remove(referable);
    myTypecheckerState.reset(referable);
  }

  /**
   * Gets the set of definitions registered in a specified module.
   *
   * @param modulePath  the module.
   *
   * @return the set of definitions registered in the module or null if the module is not registered.
   */
  @Nullable
  public Set<? extends GlobalReferable> getModuleDefinitions(ModulePath modulePath) {
    if (!isLoaded()) {
      throw new IllegalStateException("Library is not loaded");
    }

    return myModules.get(modulePath);
  }

  @Override
  public boolean load(ErrorReporter errorReporter) {
    if (isLoaded()) {
      return true;
    }

    myModules = new HashMap<>();
    SourceLoader loader = new SourceLoader(this, errorReporter);
    for (ModulePath modulePath : getModules()) {
      if (!loader.load(modulePath)) {
        unload();
        return false;
      }
    }

    return true;
  }

  @Override
  public void unload() {
    if (!isLoaded()) {
      return;
    }

    for (Set<GlobalReferable> referableSet : myModules.values()) {
      for (GlobalReferable referable : referableSet) {
        myTypecheckerState.reset(referable);
      }
    }

    myModules = null;
  }

  @Override
  public boolean isLoaded() {
    return myModules != null;
  }
}
