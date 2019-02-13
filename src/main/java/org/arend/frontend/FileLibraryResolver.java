package org.arend.frontend;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.arend.error.ErrorReporter;
import org.arend.library.FileLoadableHeaderLibrary;
import org.arend.library.Library;
import org.arend.library.LibraryConfig;
import org.arend.library.UnmodifiableSourceLibrary;
import org.arend.library.error.LibraryError;
import org.arend.library.error.LibraryIOError;
import org.arend.library.error.MultipleLibraries;
import org.arend.library.resolver.LibraryResolver;
import org.arend.typechecking.TypecheckerState;
import org.arend.util.FileUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class FileLibraryResolver implements LibraryResolver {
  private final List<Path> myLibDirs;
  private final TypecheckerState myTypecheckerState;
  private final ErrorReporter myErrorReporter;
  private final Map<String, FileLoadableHeaderLibrary> myLibraries = new HashMap<>();

  public FileLibraryResolver(List<Path> libDirs, TypecheckerState typecheckerState, ErrorReporter errorReporter) {
    myLibDirs = libDirs;
    myTypecheckerState = typecheckerState;
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
      if (config.getBinariesDir() == null) {
        config.setBinariesDir(headerFile.getParent().resolve(".bin").toString());
      }
      return new FileLoadableHeaderLibrary(config, headerFile, myTypecheckerState);
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
        myErrorReporter.report(new MultipleLibraries(libraries));
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
  public Library resolve(String name) {
    if (FileUtils.isLibraryName(name)) {
      return null;
    }

    FileLoadableHeaderLibrary library = myLibraries.get(name);
    if (library != null) {
      return library;
    }

    library = findLibrary(FileUtils.getCurrentDirectory(), name);
    if (library == null) {
      for (Path libDir : myLibDirs) {
        library = findLibrary(libDir, name);
        if (library != null) {
          break;
        }
      }
    }

    if (library == null) {
      myErrorReporter.report(LibraryError.notFound(name));
    } else {
      myLibraries.put(name, library);
    }

    return library;
  }
}
