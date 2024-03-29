package org.arend.library;

import org.arend.ext.ArendExtension;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.typechecking.DefinitionListener;
import org.arend.extImpl.DefinitionRequester;
import org.arend.library.classLoader.MultiClassLoader;
import org.arend.library.error.LibraryError;
import org.arend.library.resolver.LibraryResolver;
import org.arend.module.scopeprovider.CachingModuleScopeProvider;
import org.arend.module.scopeprovider.ModuleScopeProvider;
import org.arend.naming.scope.Scope;
import org.arend.prelude.Prelude;
import org.arend.typechecking.instance.provider.InstanceProviderSet;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;
import org.arend.util.Range;
import org.arend.util.Version;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

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
  private MultiClassLoader<Library> myExternalClassLoader = new MultiClassLoader<>(ArendExtension.class.getClassLoader());
  private MultiClassLoader<Library> myInternalClassLoader = new MultiClassLoader<>(myExternalClassLoader);
  private final DefinitionRequester myDefinitionRequester;
  private final DefinitionListener myDefinitionListener;

  /**
   * Constructs new {@code LibraryManager}.
   *
   * @param libraryResolver           a library resolver.
   * @param instanceProviderSet       an instance provider set.
   * @param typecheckingErrorReporter an error reporter for errors related to typechecking and name resolving.
   * @param libraryErrorReporter      an error reporter for errors related to loading and unloading of libraries.
   * @param definitionRequester       a listener for definitions requested in extensions.
   */
  public LibraryManager(LibraryResolver libraryResolver, @Nullable InstanceProviderSet instanceProviderSet, ErrorReporter typecheckingErrorReporter, ErrorReporter libraryErrorReporter, DefinitionRequester definitionRequester, DefinitionListener listener) {
    myLibraryResolver = libraryResolver;
    myInstanceProviderSet = instanceProviderSet;
    myTypecheckingErrorReporter = typecheckingErrorReporter;
    myLibraryErrorReporter = libraryErrorReporter;
    myDefinitionRequester = definitionRequester;
    myDefinitionListener = listener;
  }

  /**
   * Gets a module scope provider that can be used to get scopes of modules in a library and its dependencies.
   * This method may be invoked only after the library is successfully loaded.
   *
   * @param library the library.
   *
   * @return a scope provider for modules in the specified library and its dependencies.
   */
  public @NotNull ModuleScopeProvider getAvailableModuleScopeProvider(Library library) {
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

  public MultiClassLoader<Library> getClassLoader(boolean external) {
    return external ? myExternalClassLoader : myInternalClassLoader;
  }

  public DefinitionRequester getDefinitionRequester() {
    return myDefinitionRequester;
  }

  public DefinitionListener getDefinitionListener() {
    return myDefinitionListener;
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

  public void showLibraryNotFoundError(String libraryName) {
    myLibraryErrorReporter.report(LibraryError.notFound(libraryName));
  }

  public void showIncorrectLanguageVersionError(String libraryName, Range<Version> range) {
    myLibraryErrorReporter.report(LibraryError.incorrectVersion(libraryName, range));
  }

  /**
   * Loads a dependency of a given library together with its dependencies and registers them in this library manager.
   *
   * @param library         a library.
   * @param dependencyName  the name of the dependency to load.
   * @param typechecking    a typechecker that will be used for loading extensions.
   *
   * @return the loaded library if loading succeeded, null otherwise.
   */
  public Library loadDependency(Library library, String dependencyName, TypecheckingOrderingListener typechecking) {
    Library dependency = getRegisteredLibrary(dependencyName);
    if (dependency == null) {
      dependency = myLibraryResolver.resolve(library, dependencyName);
      if (dependency == null) {
        showLibraryNotFoundError(dependencyName);
        return null;
      }
    }

    return loadLibrary(dependency, typechecking) ? dependency : null;
  }

  /**
   * Loads a library together with its dependencies and registers them in this library manager.
   *
   * @param library       the library to load.
   * @param typechecking  a typechecker that will be used for loading extensions.
   *
   * @return true if loading succeeded, false otherwise.
   */
  public boolean loadLibrary(Library library, TypecheckingOrderingListener typechecking) {
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

    try {
      myReverseDependencies.put(library, new HashSet<>());
      boolean result = library.load(this, typechecking);
      if (!result) {
        myReverseDependencies.remove(library);
        myFailedLibraries.add(library);
      }
      return result;
    } finally {
      myLoadingLibraries.remove(library);
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
   * @param library         the loaded library.
   * @param loadedModules   the number of successfully loaded binary modules, or -1 if the loading failed.
   * @param totalModules    the total number of modules in the library.
   */
  protected void afterLibraryLoading(Library library, int loadedModules, int totalModules) {

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
    getClassLoader(library.isExternal()).removeDelegate(library);
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

  private void reloadLibraries(List<Library> libraries, Supplier<TypecheckingOrderingListener> supplier, boolean reloadExternal) {
    if (!myLoadingLibraries.isEmpty()) {
      myLibraryErrorReporter.report(LibraryError.unloadDuringLoading(myLoadingLibraries.stream().map(Library::getName)));
    }
    if (libraries.isEmpty()) {
      return;
    }

    for (Library library : libraries) {
      library.unload();
    }

    if (reloadExternal) {
      myExternalClassLoader = new MultiClassLoader<>(ArendExtension.class.getClassLoader());
    }
    myInternalClassLoader = new MultiClassLoader<>(myExternalClassLoader);

    TypecheckingOrderingListener typechecking = supplier.get();
    for (Library library : libraries) {
      loadLibrary(library, typechecking);
    }
  }

  /**
   * Reloads internal libraries.
   *
   * @param typechecking  a typechecker for language extensions.
   */
  public void reloadInternalLibraries(Supplier<TypecheckingOrderingListener> typechecking) {
    List<Library> libraries = new ArrayList<>();
    Iterator<Library> it = myReverseDependencies.keySet().iterator();
    while (it.hasNext()) {
      Library library = it.next();
      if (!library.isExternal()) {
        libraries.add(library);
        it.remove();
        myFailedLibraries.remove(library);
      }
    }

    reloadLibraries(libraries, typechecking, false);
  }

  /**
   * Reloads all libraries.
   *
   * @param typechecking  a typechecker for language extensions.
   */
  public void reload(Supplier<TypecheckingOrderingListener> typechecking) {
    List<Library> libraries = new ArrayList<>(myReverseDependencies.keySet());
    myFailedLibraries.clear();
    myReverseDependencies.clear();
    reloadLibraries(libraries, typechecking, true);
  }
}
