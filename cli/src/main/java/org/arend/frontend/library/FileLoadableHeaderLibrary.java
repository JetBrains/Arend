package org.arend.frontend.library;

import org.arend.ext.error.ErrorReporter;
import org.arend.library.LibraryConfig;
import org.arend.library.LibraryHeader;
import org.arend.library.classLoader.FileClassLoaderDelegate;
import org.arend.util.FileUtils;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;

public class FileLoadableHeaderLibrary extends FileSourceLibrary {
  private final LibraryConfig myConfig;
  private final Path myHeaderFile;

  public FileLoadableHeaderLibrary(LibraryConfig config, Path headerFile) {
    super(config.getName(), null, null, null);
    myConfig = config;
    myHeaderFile = headerFile;
  }

  @Override
  public String getFullName() {
    return myHeaderFile.toString();
  }

  @Nullable
  @Override
  protected LibraryHeader loadHeader(ErrorReporter errorReporter) {
    if (myConfig.getSourcesDir() != null) {
      mySourceBasePath = myHeaderFile.getParent().resolve(myConfig.getSourcesDir());
    }

    if (myConfig.getTestsDir() != null) {
      myTestBasePath = myHeaderFile.getParent().resolve(myConfig.getTestsDir());
    }

    if (myConfig.getBinariesDir() != null) {
      myBinaryBasePath = myHeaderFile.getParent().resolve(myConfig.getBinariesDir());
    }

    myLibraryHeader = LibraryHeader.fromConfig(myConfig, myHeaderFile.toString(), errorReporter);
    if (myLibraryHeader == null) {
      return null;
    }

    if (myLibraryHeader.modules == null) {
      myLibraryHeader.modules = new LinkedHashSet<>();
      if (mySourceBasePath != null) {
        FileUtils.getModules(mySourceBasePath, FileUtils.EXTENSION, myLibraryHeader.modules, errorReporter);
      }
    }

    if (myConfig.getExtensionsDir() != null) {
      myLibraryHeader.classLoaderDelegate = new FileClassLoaderDelegate(myHeaderFile.getParent().resolve(myConfig.getExtensionsDir()));
    }

    if (myTestBasePath != null) {
      myTestModules = new ArrayList<>();
      FileUtils.getModules(myTestBasePath, FileUtils.EXTENSION, myTestModules, errorReporter);
    }

    return myLibraryHeader;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FileLoadableHeaderLibrary that = (FileLoadableHeaderLibrary) o;
    return myHeaderFile.equals(that.myHeaderFile);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myHeaderFile);
  }
}
