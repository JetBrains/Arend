package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.error.DummyErrorReporter;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.namespace.SimpleDynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.frontend.namespace.SimpleStaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.frontend.resolving.OneshotSourceInfoCollector;
import com.jetbrains.jetpad.vclang.frontend.resolving.ResolvingModuleLoader;
import com.jetbrains.jetpad.vclang.frontend.storage.FileStorage;
import com.jetbrains.jetpad.vclang.frontend.storage.PreludeStorage;
import com.jetbrains.jetpad.vclang.module.DefaultModuleLoader;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.caching.CachePersistenceException;
import com.jetbrains.jetpad.vclang.module.caching.CachingModuleLoader;
import com.jetbrains.jetpad.vclang.module.caching.PersistenceProvider;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.module.source.Storage;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.term.*;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckedReporter;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.Typechecking;
import com.jetbrains.jetpad.vclang.typechecking.order.BaseDependencyListener;

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
  private final ResolvingModuleLoader<SourceIdT> resolvingModuleLoader;
  private final CachingModuleLoader<SourceIdT> moduleLoader;
  private final Map<SourceIdT, Abstract.ClassDefinition> loadedSources = new HashMap<>();

  // Name resolving
  private final NameResolver nameResolver = new NameResolver();
  private final SimpleStaticNamespaceProvider staticNsProvider = new SimpleStaticNamespaceProvider();
  private final DynamicNamespaceProvider dynamicNsProvider = new SimpleDynamicNamespaceProvider();

  private final SourceInfoProvider<SourceIdT> srcInfoProvider;

  // Typechecking
  private final TypecheckerState state;
  private final Set<SourceIdT> requestedSources = new LinkedHashSet<>();


  public BaseCliFrontend(Storage<SourceIdT> storage, boolean recompile) {
    OneshotSourceInfoCollector<SourceIdT> srcInfoCollector = new OneshotSourceInfoCollector<>();
    srcInfoProvider = srcInfoCollector.sourceInfoProvider;
    errf = new ErrorFormatter(srcInfoProvider);

    this.storage = storage;

    ModuleLoadingListener moduleLoadingListener = new ModuleLoadingListener(srcInfoCollector);
    resolvingModuleLoader = createResolvingModuleLoader(moduleLoadingListener);
    moduleLoader = createCachingModuleLoader(recompile);
    state = moduleLoader.getTypecheckerState();

    Namespace preludeNamespace = loadPrelude();
    resolvingModuleLoader.setPreludeNamespace(preludeNamespace);
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

  class ModuleLoadingListener extends DefaultModuleLoader.ModuleLoadingListener<SourceIdT> {
    private final OneshotSourceInfoCollector<SourceIdT> mySrcInfoCollector;
    private final DefinitionIdsCollector defIdCollector = new DefinitionIdsCollector();

    ModuleLoadingListener(OneshotSourceInfoCollector<SourceIdT> srcInfoCollector) {
      mySrcInfoCollector = srcInfoCollector;
    }

    @Override
    public void loadingSucceeded(SourceIdT module, Abstract.ClassDefinition abstractDefinition) {
      assert abstractDefinition != null;
      if (!definitionIds.containsKey(module)) {
        definitionIds.put(module, new HashMap<String, Abstract.Definition>());
      }
      defIdCollector.visitClass(abstractDefinition, definitionIds.get(module));
      mySrcInfoCollector.visitModule(module, abstractDefinition);
      loadedSources.put(module, abstractDefinition);
      System.out.println("[Loaded] " + displaySource(module, false));
    }
  }

  protected abstract PersistenceProvider<SourceIdT> createPersistenceProvider();
  protected abstract String displaySource(SourceIdT source, boolean modulePathOnly);


  private Namespace loadPrelude() {
    Abstract.ClassDefinition prelude = moduleLoader.load(moduleLoader.locateModule(PreludeStorage.PRELUDE_MODULE_PATH), true).definition;
    new Typechecking(state, staticNsProvider, dynamicNsProvider, new DummyErrorReporter(), new Prelude.UpdatePreludeReporter(state), new BaseDependencyListener()).typecheckModules(Collections.singletonList(prelude));
    assert errorReporter.getErrorList().isEmpty();
    return staticNsProvider.forDefinition(prelude);
  }

  private ResolvingModuleLoader<SourceIdT> createResolvingModuleLoader(ModuleLoadingListener moduleLoadingListener) {
    return new ResolvingModuleLoader<>(storage, moduleLoadingListener, nameResolver, staticNsProvider, dynamicNsProvider, new ConcreteResolveListener(errorReporter), errorReporter);
  }

  private CachingModuleLoader<SourceIdT> createCachingModuleLoader(boolean recompile) {
    CachingModuleLoader<SourceIdT> moduleLoader = new CachingModuleLoader<>(resolvingModuleLoader, createPersistenceProvider(), storage, srcInfoProvider, !recompile);
    nameResolver.setModuleLoader(moduleLoader);
    return moduleLoader;
  }

  public void run(final Path sourceDir, Collection<String> argFiles) {
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
    for (SourceIdT module : moduleLoader.getCachedModules()) {
      try {
        moduleLoader.persistModule(module);
      } catch (IOException | CachePersistenceException e) {
        e.printStackTrace();
      }
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
        return '✓';
      case GOALS:
        return '◯';
      case ERRORS:
        return '✗';
      default:
        return ' ';
    }
  }

  private Map<SourceIdT, ModuleResult> typeCheckSources(Set<SourceIdT> sources) {
    final Map<SourceIdT, ModuleResult> results = new LinkedHashMap<>();

    final Set<Abstract.ClassDefinition> modulesToTypeCheck = new LinkedHashSet<>();
    for (SourceIdT source : sources) {
      Abstract.ClassDefinition definition = loadedSources.get(source);
      if (definition == null){
        CachingModuleLoader.Result result = moduleLoader.loadWithResult(source);
        if (result.exception != null) {
          System.err.println("Error loading cache: " + result.exception);
        }
        definition = result.definition;
        flushErrors();
      }
      if (definition == null) {
        results.put(source, ModuleResult.NOT_LOADED);
        continue;
      }
      modulesToTypeCheck.add(definition);
    }

    System.out.println("--- Checking ---");

    new Typechecking(state, staticNsProvider, dynamicNsProvider, errorReporter, new TypecheckedReporter() {
      @Override
      public void typecheckingSucceeded(Abstract.Definition definition) {
        SourceIdT source = srcInfoProvider.sourceOf(definition);
        updateModuleResult(source, ModuleResult.OK);
      }

      @Override
      public void typecheckingFailed(Abstract.Definition definition) {
        SourceIdT source = srcInfoProvider.sourceOf(definition);
        boolean goals = false;
        boolean errors = false;
        for (GeneralError error : errorReporter.getErrorList()) {
          if (error.getLevel() == Error.Level.GOAL) {
            goals = true;
          }
          if (error.getLevel() == Error.Level.ERROR) {
            errors = true;
          }
        }
        updateModuleResult(source, errors ? ModuleResult.ERRORS : goals ? ModuleResult.GOALS : ModuleResult.OK);

        flushErrors();
      }

      private void updateModuleResult(SourceIdT source, ModuleResult result) {
        ModuleResult prevResult = results.get(source);
        if (prevResult == null || result.ordinal() > prevResult.ordinal()) {
          results.put(source, result);
        }
      }
    }, new BaseDependencyListener()).typecheckModules(modulesToTypeCheck);

    return results;
  }

  private void flushErrors() {
    for (GeneralError error : errorReporter.getErrorList()) {
      System.out.println(errf.printError(error));
    }
    errorReporter.getErrorList().clear();
  }

  private enum ModuleResult { UNKNOWN, OK, GOALS, NOT_LOADED, ERRORS }

  private void requestFileTypechecking(Path path) {
    String fileName = path.getFileName().toString();
    if (!fileName.endsWith(FileStorage.EXTENSION)) return;
    path = path.resolveSibling(fileName.substring(0, fileName.length() - FileStorage.EXTENSION.length()));

    ModulePath modulePath = FileStorage.modulePath(path);
    if (modulePath == null) {
      System.err.println(path  + ": illegal file name");
      return;
    }
    SourceIdT sourceId = storage.locateModule(modulePath);
    if (sourceId == null || !storage.isAvailable(sourceId)) {
      System.err.println(path  + ": source is not available");
      return;
    }
    requestedSources.add(sourceId);
  }
}
