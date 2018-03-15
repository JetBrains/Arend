package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.library.FileSourceLibrary;
import com.jetbrains.jetpad.vclang.library.Library;
import com.jetbrains.jetpad.vclang.library.UnmodifiableSourceLibrary;
import com.jetbrains.jetpad.vclang.library.error.LibraryError;
import com.jetbrains.jetpad.vclang.library.error.MultipleLibraries;
import com.jetbrains.jetpad.vclang.library.resolver.LibraryResolver;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.util.FileUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class FileLibraryResolver implements LibraryResolver {
  private final List<Path> myLibDirs;
  private final TypecheckerState myTypecheckerState;
  private final ErrorReporter myErrorReporter;
  private final Map<String, FileSourceLibrary> myLibraries = new HashMap<>();

  public FileLibraryResolver(List<Path> libDirs, TypecheckerState typecheckerState, ErrorReporter errorReporter) {
    myLibDirs = libDirs;
    myTypecheckerState = typecheckerState;
    myErrorReporter = errorReporter;
  }

  private FileSourceLibrary getLibrary(Path basePath, String libName) {
    return new FileSourceLibrary(libName, null, basePath, FileUtils.getModules(basePath, FileUtils.SERIALIZED_EXTENSION), true, Collections.emptyList(), myTypecheckerState);
  }

  private static Path findLibrary(Path libDir, String libName) {
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(libDir, file -> Files.isDirectory(file))) {
      for (Path dir : stream) {
        if (Files.exists(dir.resolve(libName + FileUtils.LIBRARY_EXTENSION))) {
          return dir;
        }
      }
    } catch (IOException e) {
      System.err.println(e.getLocalizedMessage());
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
    String libName = FileUtils.libraryName(libPath.getFileName().toString());
    if (libName != null) {
      libPath = libPath.getParent();
      if (libPath == null) {
        libPath = FileUtils.getCurrentDirectory();
      }

      FileSourceLibrary library = getLibrary(libPath, libName);
      FileSourceLibrary prevLibrary = myLibraries.putIfAbsent(libName, library);
      if (prevLibrary != null) {
        if (!prevLibrary.getBinaryBasePath().equals(library.getBinaryBasePath())) {
          List<String> libraries = new ArrayList<>(2);
          libraries.add(prevLibrary.getName() + " (" + prevLibrary.getBinaryBasePath() + ")");
          libraries.add(library.getName() + " (" + library.getBinaryBasePath() + ")");
          myErrorReporter.report(new MultipleLibraries(libraries));
          return null;
        } else {
          return prevLibrary;
        }
      } else {
        return library;
      }
    } else {
      myErrorReporter.report(LibraryError.illegalName(libPath.toString()));
      return null;
    }
  }

  @Nullable
  @Override
  public Library resolve(String name) {
    if (FileUtils.isLibraryName(name)) {
      return null;
    }

    FileSourceLibrary library = myLibraries.get(name);
    if (library != null) {
      return library;
    }

    Path libPath = findLibrary(FileUtils.getCurrentDirectory(), name);
    if (libPath == null) {
      for (Path libDir : myLibDirs) {
        libPath = findLibrary(libDir, name);
        if (libPath != null) {
          break;
        }
      }
    }

    if (libPath == null) {
      myErrorReporter.report(LibraryError.notFound(name));
    } else {
      library = getLibrary(libPath, name);
      myLibraries.put(name, library);
    }

    return library;
  }
}
