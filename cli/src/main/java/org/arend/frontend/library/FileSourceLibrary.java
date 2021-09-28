package org.arend.frontend.library;

import org.arend.ext.error.ErrorReporter;
import org.arend.ext.module.ModulePath;
import org.arend.ext.ui.ArendUI;
import org.arend.frontend.source.FileRawSource;
import org.arend.frontend.ui.ArendCliUI;
import org.arend.library.LibraryDependency;
import org.arend.library.LibraryHeader;
import org.arend.library.PersistableSourceLibrary;
import org.arend.source.*;
import org.arend.typechecking.order.dependency.DependencyListener;
import org.arend.util.Version;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FileSourceLibrary extends PersistableSourceLibrary {
  protected Path mySourceBasePath;
  protected Path myBinaryBasePath;
  protected Path myTestBasePath;
  protected LibraryHeader myLibraryHeader;
  protected List<ModulePath> myTestModules = Collections.emptyList();
  private final DependencyListener myDependencyListener;

  /**
   * Creates a new {@code UnmodifiableFileSourceLibrary}
   * @param name                the name of this library.
   * @param sourceBasePath      a path to the directory with raw source files.
   * @param binaryBasePath      a path to the directory with binary source files.
   * @param libraryHeader       specifies parameters of the library.
   */
  public FileSourceLibrary(String name, Path sourceBasePath, Path binaryBasePath, LibraryHeader libraryHeader, DependencyListener dependencyListener) {
    super(name);
    mySourceBasePath = sourceBasePath;
    myBinaryBasePath = binaryBasePath;
    myLibraryHeader = libraryHeader;
    myDependencyListener = dependencyListener;
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
  public PersistableBinarySource getPersistableBinarySource(ModulePath modulePath) {
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
    return myLibraryHeader;
  }

  @Override
  public boolean supportsPersisting() {
    return myBinaryBasePath != null && !isExternal();
  }

  @Override
  public @NotNull DependencyListener getDependencyListener() {
    return myDependencyListener;
  }

  @Override
  public @Nullable Version getVersion() {
    return myLibraryHeader == null ? null : myLibraryHeader.version;
  }

  @NotNull
  @Override
  public List<? extends LibraryDependency> getDependencies() {
    return myLibraryHeader == null ? Collections.emptyList() : myLibraryHeader.dependencies;
  }

  @Override
  public boolean containsModule(ModulePath modulePath) {
    return myLibraryHeader != null && myLibraryHeader.modules.contains(modulePath);
  }
}