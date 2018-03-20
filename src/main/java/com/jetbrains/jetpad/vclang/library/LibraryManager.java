package com.jetbrains.jetpad.vclang.library;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.library.error.LibraryError;
import com.jetbrains.jetpad.vclang.library.resolver.LibraryResolver;
import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;

import java.util.*;

/**
 * Contains all necessary information for the library loading.
 */
public class LibraryManager {
  private final LibraryResolver myLibraryResolver;
  private ModuleScopeProvider myModuleScopeProvider;
  private final ErrorReporter myTypecheckingErrorReporter;
  private final ErrorReporter myLibraryErrorReporter;
  private final Map<Library, Set<Library>> myReverseDependencies = new LinkedHashMap<>();
  private final Set<Library> myLoadingLibraries = new HashSet<>();
  private final Set<Library> myFailedLibraries = new HashSet<>();

  /**
   * Constructs new {@code LibraryManager}.
   *
   * @param libraryResolver           a library resolver.
   * @param moduleScopeProvider       a module scope provider for the whole project.
   * @param typecheckingErrorReporter an error reporter for errors related to typechecking and name resolving.
   * @param libraryErrorReporter      an error reporter for errors related to loading and unloading of libraries.
   */
  public LibraryManager(LibraryResolver libraryResolver, ModuleScopeProvider moduleScopeProvider, ErrorReporter typecheckingErrorReporter, ErrorReporter libraryErrorReporter) {
    myLibraryResolver = libraryResolver;
    myModuleScopeProvider = moduleScopeProvider;
    myTypecheckingErrorReporter = typecheckingErrorReporter;
    myLibraryErrorReporter = libraryErrorReporter;
  }

  public ModuleScopeProvider getModuleScopeProvider() {
    return myModuleScopeProvider;
  }

  public void setModuleScopeProvider(ModuleScopeProvider moduleScopeProvider) {
    myModuleScopeProvider = moduleScopeProvider;
  }

  public ErrorReporter getTypecheckingErrorReporter() {
    return myTypecheckingErrorReporter;
  }

  public ErrorReporter getLibraryErrorReporter() {
    return myLibraryErrorReporter;
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
   * Gets a set of libraries registered in this library manager.
   *
   * @return the set of registered libraries.
   */
  public Collection<? extends Library> getRegisteredLibraries() {
    return myReverseDependencies.keySet();
  }

  /**
   * Loads a library together with its dependencies and registers them in this library manager.
   *
   * @param libraryName  the name of the library to load.
   *
   * @return the loaded library if loading succeeded, null otherwise.
   */
  public Library loadLibrary(String libraryName) {
    Library library = myLibraryResolver.resolve(libraryName);
    if (library == null) {
      myLibraryErrorReporter.report(LibraryError.notFound(libraryName));
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
      myLibraryErrorReporter.report(LibraryError.cyclic(myLoadingLibraries.stream().map(Library::getName)));
      return false;
    }

    if (myReverseDependencies.containsKey(library)) {
      return true;
    }

    if (myFailedLibraries.contains(library)) {
      return false;
    }

    myLoadingLibraries.add(library);
    beforeLibraryLoading(library);
    boolean result = false;

    try {
      myReverseDependencies.put(library, new HashSet<>());
      result = library.load(this);
      if (!result) {
        myReverseDependencies.remove(library);
        myFailedLibraries.add(library);
      }
      return result;
    } finally {
      myLoadingLibraries.remove(library);
      afterLibraryLoading(library, result);
    }
  }

  /**
   * Invoked before a library begins to load.
   *
   * @param library     the loaded library.
   */
  protected void beforeLibraryLoading(Library library) {

  }

  /**
   * Invoked after a library is loaded.
   *
   * @param library     the loaded library.
   * @param successful  true if the library was successfully loaded, false otherwise.
   */
  protected void afterLibraryLoading(Library library, boolean successful) {

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
    Library library = myLibraryResolver.resolve(libraryName);
    if (library == null) {
      myLibraryErrorReporter.report(LibraryError.notFound(libraryName));
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
      myLibraryErrorReporter.report(LibraryError.unloadDuringLoading(myLoadingLibraries.stream().map(Library::getName)));
      return;
    }

    Set<Library> dependencies = myReverseDependencies.remove(library);
    if (dependencies == null) {
      return;
    }

    for (Library dependency : dependencies) {
      unloadLibrary(dependency);
    }

    library.unload();
  }
}
