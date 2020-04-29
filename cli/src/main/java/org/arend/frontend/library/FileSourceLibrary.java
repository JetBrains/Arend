package org.arend.frontend.library;

import org.arend.ext.error.ErrorReporter;
import org.arend.ext.module.ModulePath;
import org.arend.ext.ui.ArendUI;
import org.arend.frontend.source.FileRawSource;
import org.arend.frontend.ui.ArendCliUI;
import org.arend.library.LibraryDependency;
import org.arend.library.LibraryHeader;
import org.arend.library.UnmodifiableSourceLibrary;
import org.arend.source.BinarySource;
import org.arend.source.FileBinarySource;
import org.arend.source.GZIPStreamBinarySource;
import org.arend.source.Source;
import org.arend.typechecking.TypecheckerState;
import org.arend.util.Range;
import org.arend.util.Version;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class FileSourceLibrary extends UnmodifiableSourceLibrary {
  protected Path mySourceBasePath;
  protected Path myBinaryBasePath;
  protected Path myTestBasePath;
  protected Path myExtBasePath;
  protected String myExtMainClass;
  protected Set<ModulePath> myModules;
  protected List<ModulePath> myTestModules = Collections.emptyList();
  protected List<LibraryDependency> myDependencies;
  protected Range<Version> myLanguageVersion;
  protected boolean myComplete;

  /**
   * Creates a new {@code UnmodifiableFileSourceLibrary}
   * @param name              the name of this library.
   * @param sourceBasePath    a path to the directory with raw source files.
   * @param binaryBasePath    a path to the directory with binary source files.
   * @param extBasePath       a path to the directory with language extensions.
   * @param extMainClass      the main class of the language extension.
   * @param modules           the list of modules of this library.
   * @param isComplete        true if {@code modules} contains all modules of this library, false otherwise.
   * @param dependencies      the list of dependencies of this library.
   * @param languageVersion   language versions appropriate for this library.
   * @param typecheckerState  a typechecker state in which the result of loading of cached modules will be stored.
   */
  public FileSourceLibrary(String name, Path sourceBasePath, Path binaryBasePath, Path extBasePath, String extMainClass, Set<ModulePath> modules, boolean isComplete, List<LibraryDependency> dependencies, Range<Version> languageVersion, TypecheckerState typecheckerState) {
    super(name, typecheckerState);
    mySourceBasePath = sourceBasePath;
    myBinaryBasePath = binaryBasePath;
    myExtBasePath = extBasePath;
    myExtMainClass = extMainClass;
    myModules = modules;
    myComplete = isComplete;
    myLanguageVersion = languageVersion;
    myDependencies = dependencies;
  }

  public Path getSourceBasePath() {
    return mySourceBasePath;
  }

  public Path getBinaryBasePath() {
    return myBinaryBasePath;
  }

  public Path getTestBasePath() {
    return myTestBasePath;
  }

  @Nullable
  @Override
  public final Source getRawSource(ModulePath modulePath) {
    return mySourceBasePath == null ? null : new FileRawSource(mySourceBasePath, modulePath, false);
  }

  @Override
  public @Nullable Source getTestSource(ModulePath modulePath) {
    return myTestBasePath == null ? null : new FileRawSource(myTestBasePath, modulePath, true);
  }

  @Nullable
  @Override
  public BinarySource getBinarySource(ModulePath modulePath) {
    return myBinaryBasePath == null ? null : new GZIPStreamBinarySource(new FileBinarySource(myBinaryBasePath, modulePath));
  }

  @Override
  public @NotNull Collection<? extends ModulePath> getTestModules() {
    return myTestModules;
  }

  @Override
  public @Nullable ArendUI getUI() {
    return new ArendCliUI();
  }

  @Nullable
  @Override
  protected LibraryHeader loadHeader(ErrorReporter errorReporter) {
    return new LibraryHeader(myModules, myDependencies, myLanguageVersion, myExtBasePath, myExtMainClass);
  }

  @Override
  public boolean supportsPersisting() {
    return myBinaryBasePath != null && !isExternal();
  }

  @NotNull
  @Override
  public List<? extends LibraryDependency> getDependencies() {
    return myDependencies;
  }

  @Override
  public boolean containsModule(ModulePath modulePath) {
    if (myComplete) {
      return myModules.contains(modulePath);
    }

    Source source = getRawSource(modulePath);
    if (source != null) {
      return source.isAvailable();
    }

    source = getBinarySource(modulePath);
    return source != null && source.isAvailable();
  }
}