package org.arend.source;

import org.arend.ext.error.ErrorReporter;
import org.arend.ext.module.ModulePath;
import org.arend.ext.typechecking.DefinitionListener;
import org.arend.extImpl.SerializableKeyRegistryImpl;
import org.arend.library.LibraryManager;
import org.arend.library.SourceLibrary;
import org.arend.module.scopeprovider.CachingModuleScopeProvider;
import org.arend.module.scopeprovider.ModuleScopeProvider;
import org.arend.naming.reference.converter.ReferableConverter;
import org.arend.naming.scope.Scope;
import org.arend.term.group.Group;
import org.arend.typechecking.instance.provider.InstanceProviderSet;
import org.arend.typechecking.dfs.MapDFS;

import java.util.*;
import java.util.function.Function;

/**
 * Contains all necessary information for source loading.
 */
public final class SourceLoader {
  private final SourceLibrary myLibrary;
  private final ReferableConverter myReferableConverter;
  private final LibraryManager myLibraryManager;
  private ModuleScopeProvider myModuleScopeProvider;
  private ModuleScopeProvider myTestsModuleScopeProvider;

  public SourceLoader(SourceLibrary library, LibraryManager libraryManager) {
    myLibrary = library;
    myLibraryManager = libraryManager;
    myReferableConverter = myLibrary.getReferableConverter();
  }

  public SourceLibrary getLibrary() {
    return myLibrary;
  }

  public ReferableConverter getReferableConverter() {
    return myReferableConverter;
  }

  public ModuleScopeProvider getModuleScopeProvider(boolean withTests) {
    if (myModuleScopeProvider == null) {
      myModuleScopeProvider = myLibraryManager.getAvailableModuleScopeProvider(myLibrary);
    }
    if (withTests) {
      if (myTestsModuleScopeProvider == null) {
        ModuleScopeProvider testsProvider = new CachingModuleScopeProvider(myLibrary.getTestsModuleScopeProvider());
        myTestsModuleScopeProvider = module -> {
          Scope scope = myModuleScopeProvider.forModule(module);
          return scope != null ? scope : testsProvider.forModule(module);
        };
      }
      return myTestsModuleScopeProvider;
    } else {
      return myModuleScopeProvider;
    }
  }

  public InstanceProviderSet getInstanceProviderSet() {
    return myLibraryManager.getInstanceProviderSet();
  }

  public ErrorReporter getTypecheckingErrorReporter() {
    return myLibraryManager.getTypecheckingErrorReporter();
  }

  public ErrorReporter getLibraryErrorReporter() {
    return myLibraryManager.getLibraryErrorReporter();
  }

  private Set<ModulePath> loadSources(Collection<? extends ModulePath> modules, Function<ModulePath, Source> sourceMap) {
    Set<ModulePath> failed = new HashSet<>();
    Map<ModulePath, Source> sources = new LinkedHashMap<>();
    for (ModulePath module : modules) {
      Source source = sourceMap.apply(module);
      if (source != null && source.isAvailable()) {
        sources.put(module, source);
      } else {
        failed.add(module);
      }
    }

    Set<ModulePath> loaded = new HashSet<>();
    while (!sources.isEmpty()) {
      for (var it = sources.entrySet().iterator(); it.hasNext(); ) {
        Map.Entry<ModulePath, Source> entry = it.next();
        Source.LoadResult loadResult = entry.getValue().load(this);
        if (loadResult != Source.LoadResult.CONTINUE) {
          it.remove();
          if (loadResult == Source.LoadResult.FAIL) {
            failed.add(entry.getKey());
          } else {
            loaded.add(entry.getKey());
          }
        }
      }

      if (!failed.isEmpty()) {
        Map<ModulePath, List<ModulePath>> map = new HashMap<>();
        for (Map.Entry<ModulePath, Source> entry : sources.entrySet()) {
          for (ModulePath dependency : entry.getValue().getDependencies()) {
            map.computeIfAbsent(dependency, k -> new ArrayList<>()).add(entry.getKey());
          }
        }
        MapDFS<ModulePath> dfs = new MapDFS<>(map);
        for (ModulePath module : failed) {
          dfs.visit(module);
        }
        for (ModulePath module : dfs.getVisited()) {
          loaded.remove(module);
          sources.remove(module);
          Group group = myLibrary.getModuleGroup(module, false);
          if (group != null) {
            myLibrary.resetGroup(group);
          }
        }
        failed = new HashSet<>();
      }
    }

    return loaded;
  }

  /**
   * Loads raw sources that were preloaded.
   *
   * @param modules     modules to load.
   * @param inTests     true if the module located in the test directory, false otherwise.
   * @return the set of loaded modules.
   */
  public Set<ModulePath> loadRawSources(Collection<? extends ModulePath> modules, boolean inTests) {
    return loadSources(modules, module -> inTests ? myLibrary.getTestSource(module) : myLibrary.getRawSource(module));
  }

  /**
   * Loads binary modules.
   *
   * @param modules  modules to load.
   * @return the set of loaded modules.
   */
  public Set<ModulePath> loadBinarySources(Collection<? extends ModulePath> modules, SerializableKeyRegistryImpl keyRegistry, DefinitionListener definitionListener) {
    return loadSources(modules, module -> {
      BinarySource binarySource = myLibrary.getBinarySource(module);
      if (binarySource != null) {
        binarySource.setKeyRegistry(keyRegistry);
        binarySource.setDefinitionListener(definitionListener);

        if (!myLibrary.isExternal() && myLibrary.hasRawSources()) {
          Source rawSource = myLibrary.getRawSource(module);
          if (rawSource != null && rawSource.isAvailable() && binarySource.getTimeStamp() < rawSource.getTimeStamp()) {
            return null;
          }
        }
      }
      return binarySource;
    });
  }
}
