package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.error.doc.DocStringBuilder;
import com.jetbrains.jetpad.vclang.frontend.parser.SourceIdReference;
import com.jetbrains.jetpad.vclang.frontend.storage.FileStorage;
import com.jetbrains.jetpad.vclang.frontend.storage.PreludeStorage;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.caching.*;
import com.jetbrains.jetpad.vclang.module.caching.sourceless.CacheModuleScopeProvider;
import com.jetbrains.jetpad.vclang.module.caching.sourceless.CacheSourceInfoProvider;
import com.jetbrains.jetpad.vclang.module.caching.sourceless.SourcelessCacheManager;
import com.jetbrains.jetpad.vclang.module.scopeprovider.SimpleModuleScopeProvider;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.module.source.Storage;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.resolving.SimpleSourceInfoProvider;
import com.jetbrains.jetpad.vclang.term.ChildGroup;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.Typechecking;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public abstract class BaseCliFrontend<SourceIdT extends SourceId> {
  protected final ListErrorReporter errorReporter = new ListErrorReporter();

  // Modules
  protected final SimpleModuleScopeProvider sourceModuleScopeProvider = new SimpleModuleScopeProvider();
  protected final CacheModuleScopeProvider moduleScopeProvider = new CacheModuleScopeProvider(sourceModuleScopeProvider);
  protected final ModuleTracker moduleTracker;
  private final Map<SourceIdT, SourceSupplier.LoadResult> loadedSources = new HashMap<>();
  private final Set<SourceIdT> requestedSources = new LinkedHashSet<>();

  private final PrettyPrinterConfig myPrettyPrinterConfig = new PrettyPrinterConfig() {};
  protected final CacheSourceInfoProvider<SourceIdT> srcInfoProvider;
  private CacheManager<SourceIdT> cacheManager;

  // Typechecking
  private final boolean useCache;
  private TypecheckerState state;
  private Map<SourceIdT, ModuleResult> moduleResults = new LinkedHashMap<>();


  public BaseCliFrontend(boolean recompile) {
    useCache = !recompile;

    moduleTracker = new ModuleTracker();
    srcInfoProvider = new CacheSourceInfoProvider<>(moduleTracker.sourceInfoProvider);
  }

  protected void initialize(Storage<SourceIdT> storage) {
    if (cacheManager != null) {
      throw new IllegalStateException();
    }

    moduleTracker.setSourceSupplier(storage);
    cacheManager = new SourcelessCacheManager<>(storage, createModuleUriProvider(), moduleScopeProvider, srcInfoProvider, moduleTracker);
    state = cacheManager.getTypecheckerState();
  }

  class ModuleTracker extends BaseModuleLoader<SourceIdT> implements SourceVersionTracker<SourceIdT> {
    private final SimpleSourceInfoProvider<SourceIdT> sourceInfoProvider = new SimpleSourceInfoProvider<>();

    ModuleTracker() {
      super(errorReporter);
    }

    @Override
    protected void loadingSucceeded(SourceIdT module, SourceSupplier.LoadResult result) {
      sourceInfoProvider.registerModule(result.group, module);
      loadedSources.put(module, result);
      System.out.println("[Loaded] " + displaySource(module, false));
    }

    @Override
    protected void loadingFailed(SourceIdT module) {
      moduleResults.put(module, ModuleResult.NOT_LOADED);
      System.out.println("[Failed] " + displaySource(module, false));
    }

    @Override
    public ChildGroup load(SourceIdT sourceId) {
      assert !loadedSources.containsKey(sourceId);
      ModuleResult moduleResult = moduleResults.get(sourceId);
      if (moduleResult != null) {
        assert moduleResult == ModuleResult.NOT_LOADED;
        return null;
      }
      return super.load(sourceId);
    }

    public boolean isAvailable(SourceIdT sourceId) {
      return mySourceSupplier.isAvailable(sourceId);
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
      cacheLoaded = cacheManager.loadCache(sourceId);
    } catch (CacheLoadingException e) {
      cacheLoaded = false;
    }
    if (!cacheLoaded) {
      throw new IllegalStateException("Prelude cache is not available");
    }
    new Prelude.PreludeTypechecking(state).typecheckModules(Collections.singletonList(prelude));
    return prelude;
  }


  protected abstract ModuleUriProvider<SourceIdT> createModuleUriProvider();
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
            cacheManager.loadCache(source);
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

    new MyTypechecking(state).typecheckModules(modulesToTypeCheck);
  }

  private class MyTypechecking extends Typechecking {
    MyTypechecking(TypecheckerState state) {
      super(state, ConcreteReferableProvider.INSTANCE, errorReporter);
    }

    @Override
    public void typecheckingFinished(Definition definition) {
      flushErrors();
    }
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
      ModuleResult moduleResult = error.level == Error.Level.ERROR ? ModuleResult.ERRORS : error.level == Error.Level.GOAL ? ModuleResult.GOALS : null;
      if (moduleResult != null) {
        for (GlobalReferable referable : error.getAffectedDefinitions()) {
          if (referable instanceof SourceIdReference) {
            //noinspection unchecked
            updateSourceResult((SourceIdT) ((SourceIdReference) referable).sourceId, moduleResult);
          } else {
            updateSourceResult(srcInfoProvider.sourceOf(referable), moduleResult);
          }
        }
      }

      System.out.println(DocStringBuilder.build(error.getDoc(myPrettyPrinterConfig)));
    }
    errorReporter.getErrorList().clear();
  }

  private void updateSourceResult(SourceIdT source, ModuleResult result) {
    ModuleResult prevResult = moduleResults.get(source);
    if (prevResult == null || result.ordinal() > prevResult.ordinal()) {
      moduleResults.put(source, result);
    }
  }
}
