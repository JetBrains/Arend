package org.arend.frontend;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.arend.ext.error.ErrorReporter;
import org.arend.frontend.library.FileLoadableHeaderLibrary;
import org.arend.frontend.library.ZipSourceLibrary;
import org.arend.library.*;
import org.arend.library.error.LibraryIOError;
import org.arend.library.error.MultipleLibraries;
import org.arend.library.resolver.LibraryResolver;
import org.arend.typechecking.order.dependency.DependencyListener;
import org.arend.util.FileUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class FileLibraryResolver implements LibraryResolver {
  private final List<Path> myLibDirs;
  private final ErrorReporter myErrorReporter;
  private final Map<String, UnmodifiableSourceLibrary> myLibraries = new HashMap<>();
  private final DependencyListener myDependencyListener;

  public FileLibraryResolver(List<Path> libDirs, ErrorReporter errorReporter, DependencyListener dependencyListener) {
    myLibDirs = libDirs;
    myErrorReporter = errorReporter;
    myDependencyListener = dependencyListener;
  }

  private FileLoadableHeaderLibrary getLibrary(Path headerFile) {
    try {
      LibraryConfig config = new YAMLMapper().readValue(headerFile.toFile(), LibraryConfig.class);
      if (config.getName() == null) {
        Path parent = headerFile.getParent();
        Path fileName = parent == null ? null : parent.getFileName();
        if (fileName != null) {
          config.setName(fileName.toString());
        } else {
          return null;
        }
      }
      if (config.getSourcesDir() == null) {
        config.setSourcesDir(headerFile.getParent().toString());
      }
      return new FileLoadableHeaderLibrary(config, headerFile, myDependencyListener);
    } catch (IOException e) {
      myErrorReporter.report(new LibraryIOError(headerFile.toString(), "Failed to read header file", e.getLocalizedMessage()));
      return null;
    }
  }

  private ZipSourceLibrary getLibraryFromZip(Path zipFile) {
    String fileName = zipFile.getFileName().toString();
    return new ZipSourceLibrary(fileName.substring(0, fileName.length() - FileUtils.ZIP_EXTENSION.length()), zipFile.toFile());
  }

  private UnmodifiableSourceLibrary findLibrary(Path libDir, String libName) {
    UnmodifiableSourceLibrary library;
    Path zipFile = libDir.resolve(libName + FileUtils.ZIP_EXTENSION);
    if (Files.exists(zipFile)) {
      library = getLibraryFromZip(zipFile);
    } else {
      Path yaml = libDir.resolve(libName).resolve(FileUtils.LIBRARY_CONFIG_FILE);
      library = Files.exists(yaml) ? getLibrary(yaml) : null;
    }
    return library != null && library.getName().equals(libName) ? library : null;
  }

  public void addLibraryDirectory(Path libDir) {
    myLibDirs.add(libDir);
  }

  public void addLibraryDirectories(Collection<? extends Path> libDirs) {
    myLibDirs.addAll(libDirs);
  }

  /**
   * @param libPath Recommended to use a
   *                <code>.toAbsolutePath().normalize()</code> path.
   */
  public UnmodifiableSourceLibrary registerLibrary(Path libPath) {
    UnmodifiableSourceLibrary library;
    if (Files.isDirectory(libPath)) {
      library = getLibrary(libPath.resolve(FileUtils.LIBRARY_CONFIG_FILE));
    } else if (libPath.endsWith(FileUtils.LIBRARY_CONFIG_FILE)) {
      library = getLibrary(libPath);
    } else if (libPath.endsWith(FileUtils.ZIP_EXTENSION)) {
      library = getLibraryFromZip(libPath);
    } else {
      myErrorReporter.report(new LibraryIOError(libPath.toString(), "Unrecognized file type"));
      library = null;
    }

    if (library == null) {
      return null;
    }

    UnmodifiableSourceLibrary prevLibrary = myLibraries.putIfAbsent(library.getName(), library);
    if (prevLibrary != null) {
      if (!prevLibrary.equals(library)) {
        List<String> libraries = new ArrayList<>(2);
        libraries.add(prevLibrary.getName() + " (" + prevLibrary.getFullName() + ")");
        libraries.add(library.getName() + " (" + library.getFullName() + ")");
        myErrorReporter.report(new MultipleLibraries(library.getName(), libraries));
        return null;
      } else {
        return prevLibrary;
      }
    } else {
      return library;
    }
  }

  @Nullable
  @Override
  public Library resolve(Library lib, String dependencyName) {
    if (!FileUtils.isLibraryName(dependencyName)) {
      return null;
    }

    UnmodifiableSourceLibrary library = myLibraries.get(dependencyName);
    if (library != null) {
      return library;
    }

    library = findLibrary(FileUtils.getCurrentDirectory(), dependencyName);
    if (library == null) {
      for (Path libDir : myLibDirs) {
        library = findLibrary(libDir, dependencyName);
        if (library instanceof FileLoadableHeaderLibrary) {
          ((FileLoadableHeaderLibrary) library).setExternal(true);
          break;
        }
      }
    }

    if (library != null) {
      myLibraries.put(dependencyName, library);
    }

    return library;
  }
}
