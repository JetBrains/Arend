package com.jetbrains.jetpad.vclang.library;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.library.error.LibraryError;
import com.jetbrains.jetpad.vclang.library.resolver.LibraryNameResolver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Contains all necessary information for library loading.
 */
public final class LibraryManager {
  private final LibraryNameResolver myLibraryNameResolver;
  private final ErrorReporter myErrorReporter;
  private final Map<Library, Set<Library>> myReverseDependencies = new HashMap<>();
  private final Set<Library> myLoadingLibraries = new HashSet<>();

  public LibraryManager(LibraryNameResolver libraryNameResolver, ErrorReporter errorReporter) {
    myLibraryNameResolver = libraryNameResolver;
    myErrorReporter = errorReporter;
  }

  public ErrorReporter getErrorReporter() {
    return myErrorReporter;
  }

  /**
   * Checks if a library is registered in this library manager.
   *
   * @param library the library to be checked.
   *
   * @return true if the library is registered in this library manager, false otherwise.
   */
  public boolean isRegistered(Library library) {
    return myReverseDependencies.containsKey(library);
  }

  /**
   * Loads a library together with its dependencies and registers them in this library manager.
   *
   * @param libraryName  the name of the library to load.
   *
   * @return the loaded library if loading succeeded, null otherwise.
   */
  public Library loadLibrary(String libraryName) {
    Library library = myLibraryNameResolver.resolve(libraryName);
    if (library == null) {
      myErrorReporter.report(LibraryError.notFound(libraryName));
      return null;
    }

    return loadLibrary(library) ? library : null;
  }

  /**
   * Loads a library together with its dependencies and registers them in this library manager.
   *
   * @param library the library to load.
   *
   * @return true if loading succeeded, false otherwise.
   */
  public boolean loadLibrary(Library library) {
    if (myLoadingLibraries.contains(library)) {
      myErrorReporter.report(LibraryError.cyclic(myLoadingLibraries.stream().map(Library::getName)));
      return false;
    }
    myLoadingLibraries.add(library);

    if (myReverseDependencies.containsKey(library)) {
      return true;
    }

    boolean result = library.load(this);
    if (result) {
      myReverseDependencies.put(library, new HashSet<>());
    }
    myLoadingLibraries.remove(library);
    return result;
  }

  /**
   * Registers a library dependency.
   *
   * @param depender  the library that depends on another one.
   * @param dependee  the library on which the depender depends.
   *                  This library must be registered in this library manager.
   */
  public void registerDependency(Library depender, Library dependee) {
    myReverseDependencies.get(dependee).add(depender);
  }

  /**
   * Unloads a library together with all libraries depending on it and unregisters them from this library manager.
   *
   * @param libraryName  the name of the library to unload.
   */
  public void unloadLibrary(String libraryName) {
    Library library = myLibraryNameResolver.resolve(libraryName);
    if (library == null) {
      myErrorReporter.report(LibraryError.notFound(libraryName));
    } else {
      unloadLibrary(library);
    }
  }

  /**
   * Unloads a library together with all libraries depending on it and unregisters them from this library manager.
   *
   * @param library the library to unload.
   */
  public void unloadLibrary(Library library) {
    if (!myLoadingLibraries.isEmpty()) {
      myErrorReporter.report(LibraryError.unloadDuringLoading(myLoadingLibraries.stream().map(Library::getName)));
      return;
    }

    Set<Library> dependencies = myReverseDependencies.remove(library);
    if (dependencies == null) {
      return;
    }

    for (Library dependency : dependencies) {
      unloadLibrary(dependency);
    }
  }
}
