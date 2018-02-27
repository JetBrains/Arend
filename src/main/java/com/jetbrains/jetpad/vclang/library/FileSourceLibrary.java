package com.jetbrains.jetpad.vclang.library;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.source.*;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public class FileSourceLibrary extends UnmodifiableSourceLibrary {
  private final Path mySourceBasePath;
  private final Path myBinaryBasePath;
  private final List<ModulePath> myModules;
  private final List<LibraryDependency> myDependencies;

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
  public Collection<? extends ModulePath> getUpdatedModules() {
    // TODO[library]
  }
}
