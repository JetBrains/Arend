package com.jetbrains.jetpad.vclang.library;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.source.Source;
import com.jetbrains.jetpad.vclang.source.SourceLoader;
import com.jetbrains.jetpad.vclang.term.Precedence;
import com.jetbrains.jetpad.vclang.term.group.Group;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.util.LongName;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents a library which can load modules in the binary format (see {@link #getBinarySource})
 * as well as ordinary modules (see {@link #getRawSource}).
 */
public abstract class SourceLibrary extends LibraryBase {
  private boolean myRecompile = false;

  /**
   * Sets a flag so that this library will be recompiled during the loading.
   */
  public void setRecompile() {
    myRecompile = true;
  }

  /**
   * Sets a flag so that this library will not be recompiled during the loading.
   */
  public void setNoRecompile() {
    myRecompile = false;
  }

  /**
   * Creates a new {@code SourceLibrary}
   *
   * @param typecheckerState  the underling typechecker state of this library.
   */
  protected SourceLibrary(TypecheckerState typecheckerState) {
    super(typecheckerState);
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
   * Gets the binary source (that is, the source containing typechecked data) for a given module path.
   *
   * @param modulePath  a path to the source.
   *
   * @return the binary source corresponding to the given path or null if the source is not found.
   */
  @Nullable
  public abstract Source getBinarySource(ModulePath modulePath);

  /**
   * Generates a {@link GlobalReferable} for a given definition.
   * This may be a new instance or an instance representing some existing definition.
   * This method is invoked at most once for every definition.
   *
   * @param modulePath    the module of the definition.
   * @param name          the full name of the definition inside its module.
   * @param precedence    the precedence of the definition.
   * @param typecheckable the typecheckable corresponding to this definition or null if the definition is itself typecheckable.
   *
   * @return generated {@link GlobalReferable}.
   */
  @Nonnull
  protected abstract GlobalReferable generateReferable(ModulePath modulePath, LongName name, Precedence precedence, GlobalReferable typecheckable);

  /**
   * Loads the header of this library.
   *
   * @param errorReporter a reporter for all errors that occur during the loading process.
   *
   * @return loaded library header, or null if some error occurred.
   */
  @Nullable
  protected abstract LibraryHeader loadHeader(ErrorReporter errorReporter);

  /**
   * Invoked by a source after it is loaded.
   *
   * @param modulePath  the path to the loaded module.
   * @param group       the group of the loaded module.
   */
  public void onModuleLoaded(ModulePath modulePath, Group group) {

  }

  @Override
  public boolean load(LibraryManager libraryManager) {
    if (isLoaded()) {
      return true;
    }

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

    SourceLoader sourceLoader = new SourceLoader(this, libraryManager, myRecompile);
    for (ModulePath module : header.modules) {
      if (!sourceLoader.load(module)) {
        unload();
        return false;
      }
    }

    return super.load(libraryManager);
  }
}
