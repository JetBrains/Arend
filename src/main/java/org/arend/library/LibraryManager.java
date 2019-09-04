package org.arend.library;

import org.arend.error.ErrorReporter;
import org.arend.library.error.LibraryError;
import org.arend.library.resolver.LibraryResolver;
import org.arend.module.scopeprovider.CachingModuleScopeProvider;
import org.arend.module.scopeprovider.ModuleScopeProvider;
import org.arend.naming.scope.Scope;
import org.arend.prelude.Prelude;
import org.arend.typechecking.instance.provider.InstanceProviderSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

/**
 * Contains all necessary information for the library loading.
 */
public class LibraryManager {
  private final LibraryResolver myLibraryResolver;
  private final InstanceProviderSet myInstanceProviderSet;
  private final ErrorReporter myTypecheckingErrorReporter;
  private final ErrorReporter myLibraryErrorReporter;
  private final Map<Library, Set<Library>> myReverseDependencies = new LinkedHashMap<>();
  private final Set<Library> myLoadingLibraries = new HashSet<>();
  private final Set<Library> myFailedLibraries = new HashSet<>();

  /**
   * Constructs new {@code LibraryManager}.
   *
   * @param libraryResolver           a library resolver.
   * @param instanceProviderSet       an instance provider set.
   * @param typecheckingErrorReporter an error reporter for errors related to typechecking and name resolving.
   * @param libraryErrorReporter      an error reporter for errors related to loading and unloading of libraries.
   */
  public LibraryManager(LibraryResolver libraryResolver, @Nullable InstanceProviderSet instanceProviderSet, ErrorReporter typecheckingErrorReporter, ErrorReporter libraryErrorReporter) {
    myLibraryResolver = libraryResolver;
    myInstanceProviderSet = instanceProviderSet;
    myTypecheckingErrorReporter = typecheckingErrorReporter;
    myLibraryErrorReporter = libraryErrorReporter;
  }

  /**
   * Gets a module scope provider that can be used to get scopes of modules in a library and its dependencies.
   * This method may be invoked only after the library is successfully loaded.
   *
   * @param library the library.
   *
   * @return a scope provider for modules in the specified library and its dependencies.
   */
  public @Nonnull ModuleScopeProvider getAvailableModuleScopeProvider(Library library) {
    Collection<? extends LibraryDependency> dependencies = library.getDependencies();
    ModuleScopeProvider libraryModuleScopeProvider = library.getModuleScopeProvider();
    return new CachingModuleScopeProvider(modulePath -> {
      if (modulePath.equals(Prelude.MODULE_PATH)) {
        Library lib = getRegisteredLibrary(Prelude.LIBRARY_NAME);
        return lib == null ? null : lib.getModuleScopeProvider().forModule(modulePath);
      }
      Scope scope = libraryModuleScopeProvider.forModule(modulePath);
      if (scope != null) {
        return scope;
      }
      for (LibraryDependency dependency : dependencies) {
        Library lib = getRegisteredLibrary(dependency.name);
        if (lib != null) {
          scope = lib.getModuleScopeProvider().forModule(modulePath);
          if (scope != null) {
            return scope;
          }
        }
      }
      return null;
    });
  }

  public InstanceProviderSet getInstanceProviderSet() {
    return myInstanceProviderSet;
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
   * Gets the library with the given name.
   *
   * @param libraryName the name of a library.
   *
   * @return the library with the given name.
   */
  public Library getRegisteredLibrary(String libraryName) {
    for (Library library : getRegisteredLibraries()) {
      if (library.getName().equals(libraryName)) {
        return library;
      }
    }
    return null;
  }

  /**
   * Gets the library satisfying given predicate.
   *
   * @param pred  a predicate to test libraries.
   *
   * @return the library with the given name.
   */
  public Library getRegisteredLibrary(Predicate<Library> pred) {
    for (Library library : getRegisteredLibraries()) {
      if (pred.test(library)) {
        return library;
      }
    }
    return null;
  }

  /**
   * Loads a dependency of a given library together with its dependencies and registers them in this library manager.
   *
   * @param library         a library.
   * @param dependencyName  the name of the dependency to load.
   *
   * @return the loaded library if loading succeeded, null otherwise.
   */
  public Library loadDependency(Library library, String dependencyName) {
    Library dependency = myLibraryResolver.resolve(library, dependencyName);
    if (dependency == null) {
      myLibraryErrorReporter.report(LibraryError.notFound(dependencyName));
      return null;
    }

    return loadLibrary(dependency) ? dependency : null;
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
    Library library = getRegisteredLibrary(libraryName);
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
    myFailedLibraries.remove(library);
    if (!myLoadingLibraries.isEmpty()) {
      myLibraryErrorReporter.report(LibraryError.unloadDuringLoading(myLoadingLibraries.stream().map(Library::getName)));
      return;
    }

    Set<Library> dependencies = library.unload() ? myReverseDependencies.remove(library) : myReverseDependencies.get(library);
    if (dependencies != null) {
      for (Library dependency : dependencies) {
        dependency.reset();
      }
    }
  }

  /**
   * Unloads all libraries.
   */
  public void unload() {
    myFailedLibraries.clear();
    if (!myLoadingLibraries.isEmpty()) {
      myLibraryErrorReporter.report(LibraryError.unloadDuringLoading(myLoadingLibraries.stream().map(Library::getName)));
    }

    myReverseDependencies.keySet().removeIf(Library::unload);
  }
}
