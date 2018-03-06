package com.jetbrains.jetpad.vclang.library;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.source.*;
import com.jetbrains.jetpad.vclang.term.group.ChildGroup;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.Typechecking;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class FileSourceLibrary extends UnmodifiableSourceLibrary {
  private final Path mySourceBasePath;
  private final Path myBinaryBasePath;
  private final List<ModulePath> myModules;
  private final List<LibraryDependency> myDependencies;
  private final Set<ModulePath> myUpdatedModules = new LinkedHashSet<>();

  /**
   * Creates a new {@code UnmodifiableFileSourceLibrary}
   *
   * @param name              the name of this library.
   * @param sourceBasePath    a path to the directory with raw source files.
   * @param binaryBasePath    a path to the directory with binary source files.
   * @param modules           the list of modules of this library.
   * @param dependencies      the list of dependencies of this library.
   * @param typecheckerState  a typechecker state in which the result of loading of cached modules will be stored.
   */
  public FileSourceLibrary(String name, Path sourceBasePath, Path binaryBasePath, List<ModulePath> modules, List<LibraryDependency> dependencies, TypecheckerState typecheckerState) {
    super(name, typecheckerState);
    mySourceBasePath = sourceBasePath;
    myBinaryBasePath = binaryBasePath;
    myModules = modules;
    myDependencies = dependencies;
  }

  public Path getSourceBasePath() {
    return mySourceBasePath;
  }

  public Path getBinaryBasePath() {
    return myBinaryBasePath;
  }

  @Nullable
  @Override
  public final Source getRawSource(ModulePath modulePath) {
    return mySourceBasePath == null ? null : new FileRawSource(mySourceBasePath, modulePath);
  }

  @Nullable
  @Override
  public PersistableSource getBinarySource(ModulePath modulePath) {
    return myBinaryBasePath == null ? null : new GZIPStreamBinarySource(new FileBinarySource(myBinaryBasePath, modulePath));
  }

  @Nonnull
  @Override
  protected LibraryHeader loadHeader(ErrorReporter errorReporter) {
    return new LibraryHeader(myModules, myDependencies);
  }

  @Override
  public void onModuleLoaded(ModulePath modulePath, @Nullable ChildGroup group, boolean isRaw) {
    super.onModuleLoaded(modulePath, group, isRaw);
    if (isRaw) {
      myUpdatedModules.add(modulePath);
    }
  }

  @Override
  public Collection<? extends ModulePath> getUpdatedModules() {
    return myUpdatedModules;
  }

  public void updateModule(ModulePath module) {
    myUpdatedModules.add(module);
  }

  public void updateModules(Collection<? extends ModulePath> modules) {
    myUpdatedModules.addAll(modules);
  }

  @Override
  public boolean typecheck(Typechecking typechecking, ErrorReporter errorReporter) {
    if (super.typecheck(typechecking, errorReporter)) {
      myUpdatedModules.clear();
      return true;
    } else {
      return false;
    }
  }
}
