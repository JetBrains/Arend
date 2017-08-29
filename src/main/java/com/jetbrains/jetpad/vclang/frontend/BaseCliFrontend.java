package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.error.DummyErrorReporter;
import com.jetbrains.jetpad.vclang.error.ErrorClassifier;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.error.doc.DocStringBuilder;
import com.jetbrains.jetpad.vclang.frontend.parser.Position;
import com.jetbrains.jetpad.vclang.frontend.reference.GlobalReference;
import com.jetbrains.jetpad.vclang.frontend.storage.FileStorage;
import com.jetbrains.jetpad.vclang.frontend.storage.PreludeStorage;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.caching.*;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.module.source.Storage;
import com.jetbrains.jetpad.vclang.naming.FullName;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.resolving.SimpleSourceInfoProvider;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.provider.SourceInfoProvider;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckedReporter;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.Typechecking;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.TerminationCheckError;
import com.jetbrains.jetpad.vclang.typechecking.order.DependencyListener;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public abstract class BaseCliFrontend<SourceIdT extends SourceId> {
  protected final Map<SourceIdT, Map<String, GlobalReferable>> definitionIds = new HashMap<>();

  protected final ListErrorReporter<Position> errorReporter = new ListErrorReporter<>();

  // Modules
  protected final ModuleTracker moduleTracker;
  protected final Map<SourceIdT, SourceSupplier.LoadResult> loadedSources = new HashMap<>();
  private final Set<SourceIdT> requestedSources = new LinkedHashSet<>();

  private final SourceInfoProvider<SourceIdT> srcInfoProvider;
  private final CacheManager<SourceIdT> cacheManager;

  // Typechecking
  private final boolean useCache;
  private final TypecheckerState state;
  Map<SourceIdT, ModuleResult> moduleResults = new LinkedHashMap<>();


  public BaseCliFrontend(Storage<SourceIdT> storage, boolean recompile) {
    useCache = !recompile;

    moduleTracker = new ModuleTracker(storage);
    srcInfoProvider = moduleTracker.sourceInfoProvider;

    cacheManager = new CacheManager<>(createPersistenceProvider(), storage, moduleTracker, srcInfoProvider);
    state = cacheManager.getTypecheckerState();
  }

  private static void collectIds(Group group, Map<String, GlobalReferable> map) {
    Position pos = ((GlobalReference) group.getReferable()).getDefinition().getData();
    if (pos != null) {
      map.put(pos.line + ";" + pos.column, group.getReferable());
    }

    for (Group subGroup : group.getSubgroups()) {
      collectIds(subGroup, map);
    }
    for (Group subGroup : group.getDynamicSubgroups()) {
      collectIds(subGroup, map);
    }
  }

  class ModuleTracker extends BaseModuleLoader<SourceIdT> implements SourceVersionTracker<SourceIdT> {
    private final SimpleSourceInfoProvider<SourceIdT> sourceInfoProvider = new SimpleSourceInfoProvider<>();

    ModuleTracker(Storage<SourceIdT> storage) {
      super(storage, errorReporter);
    }

    @Override
    protected void loadingSucceeded(SourceIdT module, SourceSupplier.LoadResult result) {
      if (!definitionIds.containsKey(module)) {
        definitionIds.put(module, new HashMap<>());
      }
      collectIds(result.group, definitionIds.get(module));
      sourceInfoProvider.registerGroup(result.group, new FullName(result.group.getReferable().textRepresentation()), module);
      loadedSources.put(module, result);
      System.out.println("[Loaded] " + displaySource(module, false));
    }

    @Override
    protected void loadingFailed(SourceIdT module) {
      moduleResults.put(module, ModuleResult.NOT_LOADED);
      System.out.println("[Failed] " + displaySource(module, false));
    }

    @Override
    public Group load(SourceIdT sourceId) {
      assert !loadedSources.containsKey(sourceId);
      ModuleResult moduleResult = moduleResults.get(sourceId);
      if (moduleResult != null) {
        assert moduleResult == ModuleResult.NOT_LOADED;
        return null;
      }
      return super.load(sourceId);
    }

    @Override
    public Group load(ModulePath modulePath) {
      return load(locateModule(modulePath));
    }

    public SourceIdT locateModule(ModulePath modulePath) {
      SourceIdT sourceId = myStorage.locateModule(modulePath);
      if (sourceId == null) throw new IllegalStateException();
      return sourceId;
    }

    public boolean isAvailable(SourceIdT sourceId) {
      return myStorage.isAvailable(sourceId);
    }

    @Override
    public long getCurrentVersion(@Nonnull SourceIdT sourceId) {
      return loadedSources.get(sourceId).version;
    }

    @Override
    public boolean ensureLoaded(@Nonnull SourceIdT sourceId, long version) {
      SourceSupplier.LoadResult result = loadedSources.get(sourceId);
      if (result == null) throw new IllegalStateException("Cache manager trying to load a new module");
      return result.version == version;
    }
  }

  protected Group loadPrelude() {
    SourceIdT sourceId = moduleTracker.locateModule(PreludeStorage.PRELUDE_MODULE_PATH);
    Group prelude = moduleTracker.load(sourceId);
    assert errorReporter.getErrorList().isEmpty();
    boolean cacheLoaded;
    try {
      cacheLoaded = cacheManager.loadCache(sourceId, prelude.getReferable());
    } catch (CacheLoadingException e) {
      cacheLoaded = false;
    }
    if (!cacheLoaded) {
      throw new IllegalStateException("Prelude cache is not available");
    }
    new Typechecking<>(state, getStaticNsProvider(), getDynamicNsProvider(), ReferenceTypecheckableProvider.INSTANCE, new DummyErrorReporter<>(), new Prelude.UpdatePreludeReporter<>(state), new DependencyListener<Position>() {}).typecheckModules(Collections.singletonList(prelude));
    return prelude;
  }


  protected abstract StaticNamespaceProvider getStaticNsProvider();
  protected abstract DynamicNamespaceProvider getDynamicNsProvider();
  protected abstract PersistenceProvider<SourceIdT> createPersistenceProvider();
  protected abstract String displaySource(SourceIdT source, boolean modulePathOnly);


  private void requestFileTypechecking(Path path) {
    String fileName = path.getFileName().toString();
    if (!fileName.endsWith(FileStorage.EXTENSION)) return;

    Path sourcePath = path.resolveSibling(fileName.substring(0, fileName.length() - FileStorage.EXTENSION.length()));
    ModulePath modulePath = FileStorage.modulePath(sourcePath);
    if (modulePath == null) {
      System.err.println("[Not found] " + path + " is an illegal module path");
      return;
    }
    SourceIdT sourceId = moduleTracker.locateModule(modulePath);
    if (sourceId == null || !moduleTracker.isAvailable(sourceId)) {
      System.err.println("[Not found] " + path + " is not available");
      return;
    }
    requestedSources.add(sourceId);
  }

  private enum ModuleResult { UNKNOWN, OK, GOALS, NOT_LOADED, ERRORS }

  private void typeCheckSources(Set<SourceIdT> sources) {
    final Set<Group> modulesToTypeCheck = new LinkedHashSet<>();
    for (SourceIdT source : sources) {
      final Group group;
      SourceSupplier.LoadResult result = loadedSources.get(source);
      if (result == null){
        group = moduleTracker.load(source);
        if (group == null) {
          continue;
        }

        if (useCache) {
          try {
            cacheManager.loadCache(source, group.getReferable());
          } catch (CacheLoadingException e) {
            //e.printStackTrace();
          }
        }

        flushErrors();
      } else {
        group = result.group;
      }
      modulesToTypeCheck.add(group);
    }

    System.out.println("--- Checking ---");

    class ResultTracker extends ErrorClassifier<Position> implements DependencyListener<Position>, TypecheckedReporter<Position> {
      ResultTracker() {
        super(errorReporter);
      }

      @Override
      protected void reportedError(GeneralError<Position> error) {
        updateSourceResult(srcInfoProvider.sourceOf(sourceDefinitionOf(error)), ModuleResult.ERRORS);
      }

      @Override
      protected void reportedGoal(GeneralError<Position> error) {
        updateSourceResult(srcInfoProvider.sourceOf(sourceDefinitionOf(error)), ModuleResult.GOALS);
      }

      @Override
      public void alreadyTypechecked(Concrete.Definition<Position> definition) {
        Definition.TypeCheckingStatus status = state.getTypechecked(definition).status();
        if (status != Definition.TypeCheckingStatus.NO_ERRORS) {
          updateSourceResult(srcInfoProvider.sourceOf(definition), status != Definition.TypeCheckingStatus.HAS_ERRORS ? ModuleResult.ERRORS : ModuleResult.UNKNOWN);
        }
      }

      @Override
      public void typecheckingFailed(Concrete.Definition definition) {
        flushErrors();
      }

      private GlobalReferable sourceDefinitionOf(GeneralError error) {
        if (error instanceof TypeCheckingError) {
          return ((TypeCheckingError) error).definition;
        } else if (error instanceof TerminationCheckError) {
          return ((TerminationCheckError) error).definition;
        } else {
          throw new IllegalStateException("Non-typechecking error reported to typechecking reporter");
        }
      }

      private void updateSourceResult(SourceIdT source, ModuleResult result) {
        ModuleResult prevResult = moduleResults.get(source);
        if (prevResult == null || result.ordinal() > prevResult.ordinal()) {
          moduleResults.put(source, result);
        }
      }
    }
    ResultTracker resultTracker = new ResultTracker();

    new Typechecking<>(state, getStaticNsProvider(), getDynamicNsProvider(), ReferenceTypecheckableProvider.INSTANCE, resultTracker, resultTracker, resultTracker).typecheckModules(modulesToTypeCheck);
  }


  private void reportTypeCheckResult(SourceIdT source, ModuleResult result) {
    StringBuilder builder = new StringBuilder();
    builder.append("[").append(resultChar(result)).append("]");
    builder.append(" ").append(displaySource(source, true));
    System.out.println(builder);
  }

  private static char resultChar(ModuleResult result) {
    switch (result) {
      case NOT_LOADED:
        return '✗';
      case OK:
        return ' ';
      case GOALS:
        return '◯';
      case ERRORS:
        return '✗';
      default:
        return '·';
    }
  }

  public void run(final Path sourceDir, Collection<String> argFiles) {
    loadPrelude();

    // Collect sources for which typechecking was requested
    if (argFiles.isEmpty()) {
      if (sourceDir == null) return;
      try {
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
            if (path.getFileName().toString().endsWith(FileStorage.EXTENSION)) {
              requestFileTypechecking(sourceDir.relativize(path));
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
            System.err.println(e.getMessage());
            return FileVisitResult.CONTINUE;
          }
        });
      } catch (IOException e) {
        System.err.println(e.getMessage());
      }
    } else {
      for (String fileName : argFiles) {
        requestFileTypechecking(Paths.get(fileName));
      }
    }

    // Typecheck those sources
    typeCheckSources(requestedSources);
    flushErrors();

    // Output nice per-module typechecking results
    int numWithErrors = 0;
    for (Map.Entry<SourceIdT, ModuleResult> entry : moduleResults.entrySet()) {
      if (!requestedSources.contains(entry.getKey())) {
        ModuleResult result = entry.getValue();
        reportTypeCheckResult(entry.getKey(), result == ModuleResult.OK ? ModuleResult.UNKNOWN : result);
        if (result == ModuleResult.ERRORS) numWithErrors += 1;
      }
    }
    // Explicitly requested sources go last
    for (SourceIdT source : requestedSources) {
      ModuleResult result = moduleResults.get(source);
      reportTypeCheckResult(source, result == null ? ModuleResult.OK : result);
      if (result == ModuleResult.ERRORS) numWithErrors += 1;
    }
    System.out.println("--- Done ---");
    if (numWithErrors > 0) {
      System.out.println("Number of modules with errors: " + numWithErrors);
    }

    // Persist cache
    for (SourceIdT module : cacheManager.getCachedModules()) {
      try {
        cacheManager.persistCache(module);
      } catch (CachePersistenceException e) {
        e.printStackTrace();
      }
    }
  }

  private void flushErrors() {
    for (GeneralError error : errorReporter.getErrorList()) {
      System.out.println(DocStringBuilder.build(error.getDoc(srcInfoProvider)));
    }
    errorReporter.getErrorList().clear();
  }
}
