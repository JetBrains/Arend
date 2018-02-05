package com.jetbrains.jetpad.vclang.library;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.source.Source;
import com.jetbrains.jetpad.vclang.source.SourceLoader;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents a library which can load cached modules (see {@link #getCacheSource})
 * as well as ordinary modules (see {@link #getRawSource}).
 */
public abstract class SourceLibrary implements Library {
  private final String myName;
  private final TypecheckerState myTypecheckerState;
  private Map<ModulePath, Set<GlobalReferable>> myModules;

  /**
   * Creates a new {@code SourceLibrary}
   *
   * @param name              the name of this library.
   * @param typecheckerState  a typechecker state in which the result of loading of cached modules will be stored.
   */
  protected SourceLibrary(String name, TypecheckerState typecheckerState) {
    myName = name;
    myTypecheckerState = typecheckerState;
  }

  @Nonnull
  @Override
  public String getName() {
    return myName;
  }

  /**
   * Gets the raw source (that is, the source containing not typechecked data) for a given module path.
   *
   * @param modulePath  a path to the source.
   *
   * @return the raw source corresponding to the given path or null if the source is not found.
   */
  @Nullable
  public abstract Source getRawSource(ModulePath modulePath);

  /**
   * Gets the cache source (that is, the source containing typechecked data) for a given module path.
   *
   * @param modulePath  a path to the source.
   *
   * @return the cache source corresponding to the given path or null if the source is not found.
   */
  @Nullable
  public abstract Source getCacheSource(ModulePath modulePath);

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
   * Sets the group for the specified module.
   * This method is usually invoked during loading of the library.
   *
   * @param modulePath  the path to the module.
   * @param group       the group of the module.
   */
  public void setModuleGroup(ModulePath modulePath, Group group) { }

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

  /**
   * Loads the header of this library.
   *
   * @param errorReporter a reporter for all errors that occur during loading process.
   *
   * @return loaded library header, or null if some error occurred.
   */
  @Nullable
  protected abstract LibraryHeader loadHeader(ErrorReporter errorReporter);

  @Override
  public boolean load(LibraryManager libraryManager) {
    if (isLoaded()) {
      return true;
    }

    myModules = new HashMap<>();
    LibraryHeader header = loadHeader(libraryManager.getErrorReporter());
    if (header == null) {
      return false;
    }

    for (LibraryDependency dependency : header.dependencies) {
      Library loadedDependency = libraryManager.loadLibrary(dependency.name);
      if (loadedDependency == null) {
        return false;
      }
      libraryManager.registerDependency(this, loadedDependency);
    }

    SourceLoader sourceLoader = new SourceLoader(this, libraryManager.getErrorReporter());
    for (ModulePath module : header.modules) {
      if (!sourceLoader.load(module)) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SourceLibrary that = (SourceLibrary) o;

    return myName.equals(that.myName);
  }

  @Override
  public int hashCode() {
    return myName.hashCode();
  }
}
