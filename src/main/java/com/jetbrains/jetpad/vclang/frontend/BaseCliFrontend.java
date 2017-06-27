package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.error.DummyErrorReporter;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.resolving.OneshotSourceInfoCollector;
import com.jetbrains.jetpad.vclang.frontend.storage.FileStorage;
import com.jetbrains.jetpad.vclang.frontend.storage.PreludeStorage;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.caching.CacheLoadingException;
import com.jetbrains.jetpad.vclang.module.caching.CacheManager;
import com.jetbrains.jetpad.vclang.module.caching.CachePersistenceException;
import com.jetbrains.jetpad.vclang.module.caching.PersistenceProvider;
import com.jetbrains.jetpad.vclang.module.source.SimpleModuleLoader;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.module.source.SourceModuleLoader;
import com.jetbrains.jetpad.vclang.module.source.Storage;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.term.*;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckedReporter;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.Typechecking;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.TerminationCheckError;
import com.jetbrains.jetpad.vclang.typechecking.order.DependencyListener;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public abstract class BaseCliFrontend<SourceIdT extends SourceId> {
  protected final Map<SourceIdT, Map<String, Abstract.Definition>> definitionIds = new HashMap<>();

  protected final ListErrorReporter errorReporter = new ListErrorReporter();
  private final ErrorFormatter errf;

  protected final Storage<SourceIdT> storage;

  // Modules
  protected final SourceModuleLoader<SourceIdT> moduleLoader;
  protected final Map<SourceIdT, Abstract.ClassDefinition> loadedSources = new HashMap<>();
  private final Set<SourceIdT> requestedSources = new LinkedHashSet<>();

  private final SourceInfoProvider<SourceIdT> srcInfoProvider;
  private final CacheManager<SourceIdT> cacheManager;

  // Typechecking
  private final TypecheckerState state;


  public BaseCliFrontend(Storage<SourceIdT> storage, boolean recompile) {
    OneshotSourceInfoCollector<SourceIdT> srcInfoCollector = new OneshotSourceInfoCollector<>();
    srcInfoProvider = srcInfoCollector.sourceInfoProvider;
    cacheManager = new CacheManager<>(createPersistenceProvider(), storage, srcInfoProvider);
    errf = new ErrorFormatter(srcInfoProvider);

    this.storage = storage;

    moduleLoader = new ModuleWatch(new SimpleModuleLoader<>(storage, errorReporter), srcInfoCollector);
    state = cacheManager.getTypecheckerState();
  }

  static class DefinitionIdsCollector implements AbstractDefinitionVisitor<Map<String, Abstract.Definition>, Void>, AbstractStatementVisitor<Map<String, Abstract.Definition>, Void> {
    @Override
    public Void visitDefine(Abstract.DefineStatement stat, Map<String, Abstract.Definition> params) {
      Abstract.Definition definition = stat.getDefinition();
      return definition.accept(this, params);
    }

    @Override
    public Void visitNamespaceCommand(Abstract.NamespaceCommandStatement stat, Map<String, Abstract.Definition> params) {
      return null;
    }

    @Override
    public Void visitFunction(Abstract.FunctionDefinition def, Map<String, Abstract.Definition> params) {
      params.put(getIdFor(def), def);
      for (Abstract.Statement statement : def.getGlobalStatements()) {
        statement.accept(this, params);
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
      for (Abstract.Constructor constructor : def.getConstructors()) {
        constructor.accept(this, params);
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
      for (Abstract.Statement statement : def.getGlobalStatements()) {
        statement.accept(this, params);
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

  class ModuleWatch implements SourceModuleLoader<SourceIdT> {
    private final SourceModuleLoader<SourceIdT> myModuleLoader;
    private final OneshotSourceInfoCollector<SourceIdT> mySrcInfoCollector;
    private final DefinitionIdsCollector defIdCollector = new DefinitionIdsCollector();

    ModuleWatch(SourceModuleLoader<SourceIdT> moduleLoader, OneshotSourceInfoCollector<SourceIdT> srcInfoCollector) {
      myModuleLoader = moduleLoader;
      mySrcInfoCollector = srcInfoCollector;
    }


    private void loadingSucceeded(SourceIdT module, Abstract.ClassDefinition abstractDefinition) {
      assert abstractDefinition != null;
      if (!definitionIds.containsKey(module)) {
        definitionIds.put(module, new HashMap<>());
      }
      defIdCollector.visitClass(abstractDefinition, definitionIds.get(module));
      mySrcInfoCollector.visitModule(module, abstractDefinition);
      loadedSources.put(module, abstractDefinition);
      System.out.println("[Loaded] " + displaySource(module, false));
    }

    private void loadingFailed(SourceIdT module) {
      System.out.println("[Failed] " + displaySource(module, false));
    }

    @Override
    public SourceIdT locateModule(ModulePath modulePath) {
      return myModuleLoader.locateModule(modulePath);
    }

    @Override
    public boolean isAvailable(SourceIdT sourceId) {
      return myModuleLoader.isAvailable(sourceId);
    }

    @Override
    public Abstract.ClassDefinition load(SourceIdT sourceId) {
      if (loadedSources.containsKey(sourceId)) throw new IllegalStateException();
      Abstract.ClassDefinition result = myModuleLoader.load(sourceId);

      if (result != null) {
        loadingSucceeded(sourceId, result);
      } else {
        loadingFailed(sourceId);
      }

      return result;
    }
  }

  protected Abstract.ClassDefinition loadPrelude() {
    Abstract.ClassDefinition prelude = moduleLoader.load(moduleLoader.locateModule(PreludeStorage.PRELUDE_MODULE_PATH));
    assert errorReporter.getErrorList().isEmpty();
    boolean cacheLoaded;
    try {
      cacheLoaded = cacheManager.loadCache(moduleLoader.locateModule(PreludeStorage.PRELUDE_MODULE_PATH), prelude);
    } catch (CacheLoadingException e) {
      cacheLoaded = false;
    }
    if (!cacheLoaded) {
      throw new IllegalStateException("Prelude cache is not available");
    }
    new Typechecking(state, getStaticNsProvider(), getDynamicNsProvider(), new DummyErrorReporter(), new Prelude.UpdatePreludeReporter(state), new DependencyListener() {}).typecheckModules(Collections.singletonList(prelude));
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
    SourceIdT sourceId = storage.locateModule(modulePath);
    if (sourceId == null || !storage.isAvailable(sourceId)) {
      System.err.println("[Not found] " + path + " is not available");
      return;
    }
    requestedSources.add(sourceId);
  }

  private enum ModuleResult { UNKNOWN, OK, GOALS, NOT_LOADED, ERRORS }

  private Map<SourceIdT, ModuleResult> typeCheckSources(Set<SourceIdT> sources) {
    final Map<SourceIdT, ModuleResult> results = new LinkedHashMap<>();

    final Set<Abstract.ClassDefinition> modulesToTypeCheck = new LinkedHashSet<>();
    for (SourceIdT source : sources) {
      Abstract.ClassDefinition definition = loadedSources.get(source);
      if (definition == null){
        definition = moduleLoader.load(source);
        if (definition == null) {
          results.put(source, ModuleResult.NOT_LOADED);
          continue;
        }
        try {
          cacheManager.loadCache(source, definition);
        } catch (CacheLoadingException e) {
          e.printStackTrace();
        }
        flushErrors();
      }
      modulesToTypeCheck.add(definition);
    }

    System.out.println("--- Checking ---");

    class ResultTracker implements ErrorReporter, DependencyListener, TypecheckedReporter {
      @Override
      public void report(GeneralError error) {
        final ModuleResult newResult;
        switch (error.level) {
          case ERROR:
            newResult = ModuleResult.ERRORS;
            break;
          case GOAL:
            newResult = ModuleResult.GOALS;
            break;
          default:
            newResult = ModuleResult.OK;
        }

        updateSourceResult(srcInfoProvider.sourceOf(sourceDefinitionOf(error)), newResult);

        errorReporter.report(error);
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
        ModuleResult prevResult = results.get(source);
        if (prevResult == null || result.ordinal() > prevResult.ordinal()) {
          results.put(source, result);
        }
      }
    }
    ResultTracker resultTracker = new ResultTracker();

    new Typechecking(state, getStaticNsProvider(), getDynamicNsProvider(), resultTracker, resultTracker, resultTracker).typecheckModules(modulesToTypeCheck);

    return results;
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
            System.err.println(GeneralError.ioError(e));
            return FileVisitResult.CONTINUE;
          }
        });
      } catch (IOException e) {
        System.err.println(GeneralError.ioError(e));
      }
    } else {
      for (String fileName : argFiles) {
        requestFileTypechecking(Paths.get(fileName));
      }
    }

    // Typecheck those sources
    Map<SourceIdT, ModuleResult> typeCheckResults = typeCheckSources(requestedSources);
    flushErrors();

    // Output nice per-module typechecking results
    int numWithErrors = 0;
    for (Map.Entry<SourceIdT, ModuleResult> entry : typeCheckResults.entrySet()) {
      if (!requestedSources.contains(entry.getKey())) {
        ModuleResult result = entry.getValue();
        reportTypeCheckResult(entry.getKey(), result == ModuleResult.OK ? ModuleResult.UNKNOWN : result);
        if (result == ModuleResult.ERRORS) numWithErrors += 1;
      }
    }
    // Explicitly requested sources go last
    for (SourceIdT source : requestedSources) {
      ModuleResult result = typeCheckResults.get(source);
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
