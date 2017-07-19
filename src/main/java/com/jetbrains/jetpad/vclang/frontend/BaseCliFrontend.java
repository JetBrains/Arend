package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.error.*;
import com.jetbrains.jetpad.vclang.frontend.resolving.OneshotSourceInfoCollector;
import com.jetbrains.jetpad.vclang.frontend.storage.FileStorage;
import com.jetbrains.jetpad.vclang.frontend.storage.PreludeStorage;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.caching.*;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.module.source.Storage;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.SourceInfoProvider;
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
  protected final Map<SourceIdT, Map<String, Abstract.Definition>> definitionIds = new HashMap<>();

  protected final ListErrorReporter errorReporter = new ListErrorReporter();
  private final ErrorFormatter errf;

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
    srcInfoProvider = moduleTracker.sourceInfoCollector.sourceInfoProvider;

    errf = new ErrorFormatter(srcInfoProvider);

    cacheManager = new CacheManager<>(createPersistenceProvider(), storage, moduleTracker, srcInfoProvider);
    state = cacheManager.getTypecheckerState();
  }

  class ModuleTracker extends BaseModuleLoader<SourceIdT> implements SourceVersionTracker<SourceIdT> {
    private final DefinitionIdsCollector defIdCollector = new DefinitionIdsCollector();
    private final OneshotSourceInfoCollector<SourceIdT> sourceInfoCollector = new OneshotSourceInfoCollector<>();

    ModuleTracker(Storage<SourceIdT> storage) {
      super(storage, errorReporter);
    }

    @Override
    protected void loadingSucceeded(SourceIdT module, SourceSupplier.LoadResult result) {
      if (!definitionIds.containsKey(module)) {
        definitionIds.put(module, new HashMap<>());
      }
      defIdCollector.visitClass(result.definition, definitionIds.get(module));
      sourceInfoCollector.visitModule(module, result.definition);
      loadedSources.put(module, result);
      System.out.println("[Loaded] " + displaySource(module, false));
    }

    @Override
    protected void loadingFailed(SourceIdT module) {
      moduleResults.put(module, ModuleResult.NOT_LOADED);
      System.out.println("[Failed] " + displaySource(module, false));
    }

    @Override
    public Abstract.ClassDefinition load(SourceIdT sourceId) {
      assert !loadedSources.containsKey(sourceId);
      ModuleResult moduleResult = moduleResults.get(sourceId);
      if (moduleResult != null) {
        assert moduleResult == ModuleResult.NOT_LOADED;
        return null;
      }
      return super.load(sourceId);
    }

    @Override
    public Abstract.ClassDefinition load(ModulePath modulePath) {
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

  static class DefinitionIdsCollector implements AbstractDefinitionVisitor<Map<String, Abstract.Definition>, Void> {

    @Override
    public Void visitFunction(Abstract.FunctionDefinition def, Map<String, Abstract.Definition> params) {
      params.put(getIdFor(def), def);
      for (Abstract.Definition definition : def.getGlobalDefinitions()) {
        definition.accept(this, params);
      }
      return null;
    }

    @Override
    public Void visitClassField(Abstract.ClassField def, Map<String, Abstract.Definition> params) {
      params.put(getIdFor(def), def);
      return null;
    }

    @Override
    public Void visitData(Abstract.DataDefinition def, Map<String, Abstract.Definition> params) {
      params.put(getIdFor(def), def);
      for (Abstract.ConstructorClause clause : def.getConstructorClauses()) {
        for (Abstract.Constructor constructor : clause.getConstructors()) {
          constructor.accept(this, params);
        }
      }
      return null;
    }

    @Override
    public Void visitConstructor(Abstract.Constructor def, Map<String, Abstract.Definition> params) {
      params.put(getIdFor(def), def);
      return null;
    }

    @Override
    public Void visitClass(Abstract.ClassDefinition def, Map<String, Abstract.Definition> params) {
      params.put(getIdFor(def), def);
      for (Abstract.Definition definition : def.getGlobalDefinitions()) {
        definition.accept(this, params);
      }
      for (Abstract.Definition definition : def.getInstanceDefinitions()) {
        definition.accept(this, params);
      }
      for (Abstract.ClassField field : def.getFields()) {
        field.accept(this, params);
      }

      return null;
    }

    @Override
    public Void visitImplement(Abstract.Implementation def, Map<String, Abstract.Definition> params) {
      return null;
    }

    @Override
    public Void visitClassView(Abstract.ClassView def, Map<String, Abstract.Definition> params) {
      return null;
    }

    @Override
    public Void visitClassViewField(Abstract.ClassViewField def, Map<String, Abstract.Definition> params) {
      return null;
    }

    @Override
    public Void visitClassViewInstance(Abstract.ClassViewInstance def, Map<String, Abstract.Definition> params) {
      params.put(getIdFor(def), def);
      return null;
    }


    static String getIdFor(Abstract.Definition definition) {
      if (definition instanceof Concrete.Definition) {
        Concrete.Position pos = ((Concrete.Definition) definition).getPosition();
        if (pos != null) {
          return pos.line + ";" + pos.column;
        }
      }
      return null;
    }
  }

  protected Abstract.ClassDefinition loadPrelude() {
    SourceIdT sourceId = moduleTracker.locateModule(PreludeStorage.PRELUDE_MODULE_PATH);
    Abstract.ClassDefinition prelude = moduleTracker.load(sourceId);
    assert errorReporter.getErrorList().isEmpty();
    boolean cacheLoaded;
    try {
      cacheLoaded = cacheManager.loadCache(sourceId, prelude);
    } catch (CacheLoadingException e) {
      cacheLoaded = false;
    }
    if (!cacheLoaded) {
      throw new IllegalStateException("Prelude cache is not available");
    }
    new Typechecking(state, getStaticNsProvider(), getDynamicNsProvider(), Concrete.NamespaceCommandStatement.GET, new DummyErrorReporter(), new Prelude.UpdatePreludeReporter(state), new DependencyListener() {}).typecheckModules(Collections.singletonList(prelude));
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
    final Set<Abstract.ClassDefinition> modulesToTypeCheck = new LinkedHashSet<>();
    for (SourceIdT source : sources) {
      final Abstract.ClassDefinition definition;
      SourceSupplier.LoadResult result = loadedSources.get(source);
      if (result == null){
        definition = moduleTracker.load(source);
        if (definition == null) {
          continue;
        }

        if (useCache) {
          try {
            cacheManager.loadCache(source, definition);
          } catch (CacheLoadingException e) {
            //e.printStackTrace();
          }
        }

        flushErrors();
      } else {
        definition = result.definition;
      }
      modulesToTypeCheck.add(definition);
    }

    System.out.println("--- Checking ---");

    class ResultTracker extends ErrorClassifier implements DependencyListener, TypecheckedReporter {
      ResultTracker() {
        super(errorReporter);
      }

      @Override
      protected void reportedError(GeneralError error) {
        updateSourceResult(srcInfoProvider.sourceOf(sourceDefinitionOf(error)), ModuleResult.ERRORS);
      }

      @Override
      protected void reportedGoal(GeneralError error) {
        updateSourceResult(srcInfoProvider.sourceOf(sourceDefinitionOf(error)), ModuleResult.GOALS);
      }

      @Override
      public void alreadyTypechecked(Abstract.Definition definition) {
        Definition.TypeCheckingStatus status = state.getTypechecked(definition).status();
        if (status != Definition.TypeCheckingStatus.NO_ERRORS) {
          updateSourceResult(srcInfoProvider.sourceOf(definition), status != Definition.TypeCheckingStatus.HAS_ERRORS ? ModuleResult.ERRORS : ModuleResult.UNKNOWN);
        }
      }

      @Override
      public void typecheckingFailed(Abstract.Definition definition) {
        flushErrors();
      }

      private Abstract.Definition sourceDefinitionOf(GeneralError error) {
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

    new Typechecking(state, getStaticNsProvider(), getDynamicNsProvider(), Concrete.NamespaceCommandStatement.GET, resultTracker, resultTracker, resultTracker).typecheckModules(modulesToTypeCheck);
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
      System.out.println(errf.printError(error));
    }
    errorReporter.getErrorList().clear();
  }
}
