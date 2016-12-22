package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.error.DummyErrorReporter;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.module.DefaultModuleLoader;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.caching.CachePersistenceException;
import com.jetbrains.jetpad.vclang.module.caching.CachingModuleLoader;
import com.jetbrains.jetpad.vclang.module.caching.PersistenceProvider;
import com.jetbrains.jetpad.vclang.module.source.CompositeSourceSupplier;
import com.jetbrains.jetpad.vclang.module.source.CompositeStorage;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.frontend.namespace.SimpleDynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.frontend.namespace.SimpleStaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.frontend.resolving.OneshotSourceInfoCollector;
import com.jetbrains.jetpad.vclang.frontend.resolving.ResolvingModuleLoader;
import com.jetbrains.jetpad.vclang.term.*;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.statement.visitor.AbstractStatementVisitor;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckedReporter;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.Typechecking;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class ConsoleMain {
  private static Options cmdOptions = new Options();
  static {
    cmdOptions.addOption("h", "help", false, "print this message");
    cmdOptions.addOption(Option.builder("s").longOpt("source").hasArg().argName("dir").desc("source directory").build());
    //cmdOptions.addOption(Option.builder("o").longOpt("output").hasArg().argName("dir").desc("output directory").build());
    //cmdOptions.addOption(Option.builder("L").hasArg().argName("dir").desc("add <dir> to the list of directories searched for libraries").build());
    cmdOptions.addOption(Option.builder().longOpt("recompile").desc("recompile files").build());
  }

  private final List<String> argFiles;

  private final ListErrorReporter errorReporter = new ListErrorReporter();
  private final ErrorFormatter errf;

  // Storage
  private final String sourceDirStr;
  private final Path sourceDir;
  private final PreludeStorage preludeStorage;
  private final FileStorage fileStorage;
  private final CompositeStorage<PreludeStorage.SourceId, FileStorage.SourceId> storage;

  // Modules
  private final ResolvingModuleLoader<CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId> resolvingModuleLoader;
  private final CachingModuleLoader<CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId> moduleLoader;
  private final Map<CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId, Map<String, Abstract.Definition>> definitionIds = new HashMap<>();

  // Name resolving
  private final SimpleStaticNamespaceProvider staticNsProvider  = new SimpleStaticNamespaceProvider();
  private final DynamicNamespaceProvider dynamicNsProvider = new SimpleDynamicNamespaceProvider();

  private final SourceInfoProvider<CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId> srcInfoProvider;

  // Typechecking
  private final TypecheckerState state;
  private final Set<CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId> requestedSources = new LinkedHashSet<>();


  public ConsoleMain(CommandLine cmdLine) {
    OneshotSourceInfoCollector<CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId> srcInfoCollector = new OneshotSourceInfoCollector<>();
    srcInfoProvider = srcInfoCollector.sourceInfoProvider;
    errf = new ErrorFormatter(srcInfoProvider);

    preludeStorage = new PreludeStorage();
    sourceDirStr = cmdLine.getOptionValue("s");
    sourceDir = Paths.get(sourceDirStr == null ? System.getProperty("user.dir") : sourceDirStr);
    fileStorage = new FileStorage(sourceDir);
    storage = new CompositeStorage<>(preludeStorage, fileStorage, preludeStorage, fileStorage);

    boolean recompile = cmdLine.hasOption("recompile");
    ModuleLoadingListener moduleLoadingListener = new ModuleLoadingListener(srcInfoCollector);
    resolvingModuleLoader = createResolvingModuleLoader(moduleLoadingListener);
    moduleLoader = createCachingModuleLoader(recompile);
    state = moduleLoader.getTypecheckerState();

    Namespace preludeNamespace = loadPrelude();
    resolvingModuleLoader.setPreludeNamespace(preludeNamespace);

    argFiles = cmdLine.getArgList();
  }

  private ResolvingModuleLoader<CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId> createResolvingModuleLoader(ModuleLoadingListener moduleLoadingListener) {
    return new ResolvingModuleLoader<>(storage, moduleLoadingListener, staticNsProvider, dynamicNsProvider, new ConcreteResolveListener(), errorReporter);
  }

  private CachingModuleLoader<CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId> createCachingModuleLoader(boolean recompile) {
    CachingModuleLoader<CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId> moduleLoader = new CachingModuleLoader<>(resolvingModuleLoader, new MyPersistenceProvider(), storage, srcInfoProvider, !recompile);
    resolvingModuleLoader.overrideModuleLoader(moduleLoader);
    return moduleLoader;
  }

  private Namespace loadPrelude() {
    Abstract.ClassDefinition prelude = moduleLoader.load(moduleLoader.locateModule(PreludeStorage.PRELUDE_MODULE_PATH), true).definition;
    Typechecking.typecheckModules(state, staticNsProvider, dynamicNsProvider, Collections.singletonList(prelude), new DummyErrorReporter(), new Prelude.UpdatePreludeReporter(state));
    assert errorReporter.getErrorList().isEmpty();
    return staticNsProvider.forDefinition(prelude);
  }

  private void run() {
    // Collect sources for which typechecking was requested
    if (argFiles.isEmpty()) {
      if (sourceDirStr == null) return;
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
    Map<CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId, ModuleResult> typeCheckResults = typeCheckSources(requestedSources);

    flushErrors();

    // Output nice per-module typechecking results
    int numWithErrors = 0;
    for (Map.Entry<CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId, ModuleResult> entry : typeCheckResults.entrySet()) {
      if (!requestedSources.contains(entry.getKey())) {
        ModuleResult result = entry.getValue();
        reportTypeCheckResult(entry.getKey(), result == ModuleResult.OK ? ModuleResult.UNKNOWN : result);
        if (result == ModuleResult.ERRORS) numWithErrors += 1;
      }
    }
    for (CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId source : requestedSources) {
      ModuleResult result = typeCheckResults.get(source);
      reportTypeCheckResult(source, result == null ? ModuleResult.OK : result);
      if (result == ModuleResult.ERRORS) numWithErrors += 1;
    }
    System.out.println("--- Done ---");
    if (numWithErrors > 0) {
      System.out.println("Number of modules with errors: " + numWithErrors);
    }

    // Persist cache
    for (CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId module : moduleLoader.getCachedModules()) {
      try {
        moduleLoader.persistModule(module);
      } catch (IOException | CachePersistenceException e) {
        e.printStackTrace();
      }
    }
  }

  private void reportTypeCheckResult(CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId source, ModuleResult result) {
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

  private Map<CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId, ModuleResult> typeCheckSources(Set<CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId> sources) {
    final Map<CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId, ModuleResult> results = new LinkedHashMap<>();

    final Set<Abstract.ClassDefinition> modulesToTypeCheck = new LinkedHashSet<>();
    for (CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId source : sources) {
      Abstract.ClassDefinition definition = resolvingModuleLoader.getLoadedModule(source.getModulePath());
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

    Typechecking.typecheckModules(state, staticNsProvider, dynamicNsProvider, modulesToTypeCheck, errorReporter, new TypecheckedReporter() {
      @Override
      public void typecheckingSucceeded(Abstract.Definition definition) {
        CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId source = srcInfoProvider.sourceOf(definition);
        updateModuleResult(source, ModuleResult.OK);
      }

      @Override
      public void typecheckingFailed(Abstract.Definition definition) {
        CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId source = srcInfoProvider.sourceOf(definition);
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

      private void updateModuleResult(CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId source, ModuleResult result) {
        ModuleResult prevResult = results.get(source);
        if (prevResult == null || result.ordinal() > prevResult.ordinal()) {
          results.put(source, result);
        }
      }
    });

    return results;
  }

  private void flushErrors() {
    for (GeneralError error : errorReporter.getErrorList()) {
      System.out.println(errf.printError(error));
    }
    errorReporter.getErrorList().clear();
  }

  private enum ModuleResult { UNKNOWN, OK, GOALS, NOT_LOADED, ERRORS }

  class ModuleLoadingListener extends DefaultModuleLoader.ModuleLoadingListener<CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId> {
    private final OneshotSourceInfoCollector<CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId> mySrcInfoCollector;
    private final DefinitionIdsCollector defIdCollector = new DefinitionIdsCollector();

    ModuleLoadingListener(OneshotSourceInfoCollector<CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId> srcInfoCollector) {
      mySrcInfoCollector = srcInfoCollector;
    }

    @Override
    public void loadingSucceeded(CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId module, Abstract.ClassDefinition abstractDefinition) {
      assert abstractDefinition != null;
      if (!definitionIds.containsKey(module)) {
        definitionIds.put(module, new HashMap<String, Abstract.Definition>());
      }
      defIdCollector.visitClass(abstractDefinition, definitionIds.get(module));
      mySrcInfoCollector.visitModule(module, abstractDefinition);
      System.out.println("[Loaded] " + displaySource(module, false));
    }
  }

  private static String displaySource(CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId source, boolean modulePathOnly) {
    StringBuilder builder = new StringBuilder();
    builder.append(source.getModulePath());
    if (!modulePathOnly && source.source2 != null) {
      builder.append(" (").append(source.source2).append(")");
    }
    return builder.toString();
  }

  private void requestFileTypechecking(Path path) {
    String fileName = path.getFileName().toString();
    if (!fileName.endsWith(FileStorage.EXTENSION)) return;
    path = path.resolveSibling(fileName.substring(0, fileName.length() - FileStorage.EXTENSION.length()));

    ModulePath modulePath = FileStorage.modulePath(path);
    if (modulePath == null) {
      System.err.println(path  + ": illegal file name");
      return;
    }
    CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId sourceId = storage.locateModule(modulePath);
    if (sourceId == null || !storage.isAvailable(sourceId)) {
      System.err.println(path  + ": source is not available");
      return;
    }
    requestedSources.add(sourceId);
  }

  class MyPersistenceProvider implements PersistenceProvider<CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId> {
    @Override
    public URL getUrl(CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId sourceId) {
      try {
        final String root;
        final Path relPath;
        final String query;
        if (sourceId.source1 != null) {
          root = "prelude";
          relPath = Paths.get("");
          query = null;
        } else {
          root = "";
          relPath = sourceId.source2.getRelativeFilePath();
          query = "" + sourceId.source2.getLastModified();
        }
        return new URI("file", root, Paths.get("/").resolve(relPath).toUri().getPath(), query, null).toURL();
      } catch (URISyntaxException | MalformedURLException e) {
        throw new IllegalStateException();
      }
    }

    @Override
    public CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId getModuleId(URL sourceUrl) {
      if (sourceUrl.getAuthority() != null && sourceUrl.getAuthority().equals("prelude")) {
        if (sourceUrl.getPath().equals("/")) {
          return storage.idFromFirst(preludeStorage.preludeSourceId);
        } else {
          return null;
        }
      } else if (sourceUrl.getAuthority() == null) {
        try {
          Path path = Paths.get(new URI(sourceUrl.getProtocol(), null, sourceUrl.getPath(), null));
          ModulePath modulePath = FileStorage.modulePath(path.getRoot().relativize(path));
          if (modulePath == null) return null;

          final FileStorage.SourceId fileSourceId;
          if (sourceUrl.getQuery() == null) {
            fileSourceId = fileStorage.locateModule(modulePath);
          } else {
            long mtime = Long.parseLong(sourceUrl.getQuery());
            fileSourceId = fileStorage.locateModule(modulePath, mtime);
          }
          return fileSourceId != null ? storage.idFromSecond(fileSourceId) : null;
        } catch (URISyntaxException | NumberFormatException e) {
          return null;
        }
      } else {
        return null;
      }
    }

    @Override
    public String getIdFor(Abstract.Definition definition) {
      if (definition instanceof Concrete.Definition) {
        Concrete.Position pos = ((Concrete.Definition) definition).getPosition();
        if (pos != null) {
          return pos.line + ";" + pos.column;
        }
      }
      return null;
    }

    @Override
    public Abstract.Definition getFromId(CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId sourceId, String id) {
      Map<String, Abstract.Definition> sourceMap = definitionIds.get(sourceId);
      if (sourceMap == null) {
        return null;
      } else {
        return sourceMap.get(id);
      }
    }
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
      for (Abstract.Statement statement : def.getStatements()) {
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


  public static void main(String[] args) {
    try {
      CommandLine cmdLine = new DefaultParser().parse(cmdOptions, args);

      if (cmdLine.hasOption("h")) {
        printHelp();
      } else {
        new ConsoleMain(cmdLine).run();
      }
    } catch (ParseException e) {
      System.err.println(e.getMessage());
    }
  }

  private static void printHelp() {
    new HelpFormatter().printHelp("vclang [FILES]", cmdOptions);
  }
}
