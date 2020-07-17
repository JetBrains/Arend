package org.arend.frontend;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.arend.ext.error.ErrorReporter;
import org.arend.frontend.library.FileLoadableHeaderLibrary;
import org.arend.library.Library;
import org.arend.library.LibraryConfig;
import org.arend.library.UnmodifiableSourceLibrary;
import org.arend.library.error.LibraryIOError;
import org.arend.library.error.MultipleLibraries;
import org.arend.library.resolver.LibraryResolver;
import org.arend.util.FileUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class FileLibraryResolver implements LibraryResolver {
  private final List<Path> myLibDirs;
  private final ErrorReporter myErrorReporter;
  private final Map<String, FileLoadableHeaderLibrary> myLibraries = new HashMap<>();

  public FileLibraryResolver(List<Path> libDirs, ErrorReporter errorReporter) {
    myLibDirs = libDirs;
    myErrorReporter = errorReporter;
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
      return new FileLoadableHeaderLibrary(config, headerFile);
    } catch (IOException e) {
      myErrorReporter.report(new LibraryIOError(headerFile.toString(), "Failed to read header file", e.getLocalizedMessage()));
      return null;
    }
  }

  private FileLoadableHeaderLibrary findLibrary(Path libDir, String libName) {
    Path header = libDir.resolve(libName).resolve(FileUtils.LIBRARY_CONFIG_FILE);
    if (Files.exists(header)) {
      FileLoadableHeaderLibrary library = getLibrary(header);
      if (library != null && library.getName().equals(libName)) {
        return library;
      }
    }
    return null;
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
    if (Files.isDirectory(libPath)) {
      libPath = libPath.resolve(FileUtils.LIBRARY_CONFIG_FILE);
    }

    FileLoadableHeaderLibrary library = getLibrary(libPath);
    if (library == null) {
      return null;
    }

    FileLoadableHeaderLibrary prevLibrary = myLibraries.putIfAbsent(library.getName(), library);
    if (prevLibrary != null) {
      if (!prevLibrary.getHeaderFile().equals(library.getHeaderFile())) {
        List<String> libraries = new ArrayList<>(2);
        libraries.add(prevLibrary.getName() + " (" + prevLibrary.getHeaderFile() + ")");
        libraries.add(library.getName() + " (" + library.getHeaderFile() + ")");
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

    FileLoadableHeaderLibrary library = myLibraries.get(dependencyName);
    if (library != null) {
      return library;
    }

    library = findLibrary(FileUtils.getCurrentDirectory(), dependencyName);
    if (library == null) {
      for (Path libDir : myLibDirs) {
        library = findLibrary(libDir, dependencyName);
        if (library != null) {
          library.setExternal(true);
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
