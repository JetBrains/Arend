package org.arend.frontend.repl;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.ListErrorReporter;
import org.arend.ext.module.ModulePath;
import org.arend.extImpl.DefinitionRequester;
import org.arend.frontend.ConcreteReferableProvider;
import org.arend.frontend.FileLibraryResolver;
import org.arend.frontend.PositionComparator;
import org.arend.frontend.library.FileSourceLibrary;
import org.arend.frontend.library.TimedLibraryManager;
import org.arend.frontend.parser.ArendLexer;
import org.arend.frontend.parser.ArendParser;
import org.arend.frontend.parser.BuildVisitor;
import org.arend.frontend.parser.ReporterErrorListener;
import org.arend.frontend.repl.action.*;
import org.arend.library.Library;
import org.arend.library.LibraryManager;
import org.arend.library.SourceLibrary;
import org.arend.module.ModuleLocation;
import org.arend.naming.reference.converter.IdReferableConverter;
import org.arend.naming.scope.Scope;
import org.arend.prelude.GeneratedVersion;
import org.arend.prelude.PreludeLibrary;
import org.arend.prelude.PreludeResourceLibrary;
import org.arend.repl.Repl;
import org.arend.repl.action.NormalizeCommand;
import org.arend.repl.action.ReplCommand;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.FileGroup;
import org.arend.typechecking.LibraryArendExtensionProvider;
import org.arend.typechecking.SimpleTypecheckerState;
import org.arend.typechecking.instance.provider.InstanceProviderSet;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;
import org.arend.typechecking.provider.ConcreteProvider;
import org.arend.util.FileUtils;
import org.arend.util.Range;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;

public abstract class CommonCliRepl extends Repl {
  private final Properties properties = new Properties();
  public static final @NotNull Path USER_HOME = Paths.get(System.getProperty("user.home")).toAbsolutePath().normalize();
  public static final @NotNull String APP_NAME = "Arend REPL";

  public @NotNull Path pwd = Paths.get("").toAbsolutePath();
  /**
   * See https://gist.github.com/ice1000/a915b6fcbc6f90b0c3c65db44dab29cc
   */
  @Language("TEXT")
  public static final @NotNull String ASCII_BANNER =
      "    ___                       __\n" +
      "   /   |  _______  ____  ____/ /\n" +
      "  / /| | / __/ _ \\/ __ \\/ __  /  " + APP_NAME + " " + GeneratedVersion.VERSION_STRING + "\n" +
      " / ___ |/ / /  __/ / / / /_/ /   https://arend-lang.github.io\n" +
      "/_/  |_/_/  \\___/_/ /_/\\__,_/    :? for help";

  @NotNull
  protected String prompt = ">";
  private final FileLibraryResolver myLibraryResolver;
  private final SourceLibrary myReplLibrary;
  private final Set<ModulePath> myModules;

  //region Tricky constructors (expand to read more...)
  // These two constructors are used for convincing javac that the
  // initialization is of well order.
  // All of the parameters introduced here are used more than once,
  // and one cannot introduce them as variable before the `this` or
  // `super` call because that's the rule of javac.
  private CommonCliRepl(
      @NotNull SimpleTypecheckerState typecheckerState,
      @NotNull Set<ModulePath> modules,
      @NotNull ListErrorReporter errorReporter) {
    this(
        typecheckerState,
        modules,
        new FileLibraryResolver(new ArrayList<>(), typecheckerState, errorReporter),
        new InstanceProviderSet(),
        errorReporter
    );
  }

  private CommonCliRepl(
      @NotNull SimpleTypecheckerState typecheckerState,
      @NotNull Set<ModulePath> modules,
      @NotNull FileLibraryResolver libraryResolver,
      @NotNull InstanceProviderSet instanceProviders,
      @NotNull ListErrorReporter errorReporter) {
    this(typecheckerState,
      libraryManager(libraryResolver, instanceProviders, errorReporter),
      modules,
      libraryResolver,
      ConcreteReferableProvider.INSTANCE,
      instanceProviders,
      errorReporter);
  }

  private CommonCliRepl(
      @NotNull SimpleTypecheckerState typecheckerState,
      @NotNull LibraryManager libraryManager,
      @NotNull Set<ModulePath> modules,
      @NotNull FileLibraryResolver libraryResolver,
      @NotNull ConcreteProvider concreteProvider,
      @NotNull InstanceProviderSet instanceProviders,
      @NotNull ListErrorReporter errorReporter) {
    super(
      errorReporter,
      libraryManager,
      concreteProvider,
      new TypecheckingOrderingListener(instanceProviders, typecheckerState, concreteProvider, IdReferableConverter.INSTANCE, errorReporter, PositionComparator.INSTANCE, new LibraryArendExtensionProvider(libraryManager)), typecheckerState
    );
    myLibraryResolver = libraryResolver;
    myReplLibrary = Files.exists(pwd.resolve(FileUtils.LIBRARY_CONFIG_FILE))
        ? libraryResolver.registerLibrary(pwd)
        : new FileSourceLibrary("Repl", pwd, null, null, null, modules, true, new ArrayList<>(), Range.unbound(), typecheckerState);
    myModules = modules;
  }
  //endregion

  public static final @NotNull String NORMALIZATION_KEY = "normalization";
  public static final @NotNull String PROMPT_KEY = "prompt";
  public static final @NotNull String REPL_CONFIG_FILE = "repl_config.properties";
  private static final Path config = USER_HOME.resolve(FileUtils.USER_CONFIG_DIR).resolve(REPL_CONFIG_FILE);

  {
    try {
      if (Files.exists(config)) {
        properties.load(Files.newInputStream(config));
        var normalization = String.valueOf(properties.get(NORMALIZATION_KEY));
        NormalizeCommand.INSTANCE.loadNormalize(normalization, this, false);
        var promptConfig = properties.get(PROMPT_KEY);
        if (promptConfig != null) prompt = String.valueOf(promptConfig);
      }
    } catch (IOException e) {
      eprintln("[ERROR] Failed to load repl config: " + e.getLocalizedMessage());
    }
  }

  public final void saveUserConfig() {
    try {
      if (Files.notExists(config)) Files.createFile(config);
      properties.setProperty(NORMALIZATION_KEY, String.valueOf(normalizationMode));
      properties.setProperty(PROMPT_KEY, prompt);
      try (var out = Files.newOutputStream(config)) {
        properties.store(out, "Created automatically by Arend REPL");
      }
    } catch (IOException e) {
      eprintln("[ERROR] Failed to save repl config: " + e.getLocalizedMessage());
    }
  }

  @NotNull
  private static TimedLibraryManager libraryManager(@NotNull FileLibraryResolver libraryResolver, @NotNull InstanceProviderSet instanceProviders, @NotNull ListErrorReporter errorReporter) {
    return new TimedLibraryManager(libraryResolver, instanceProviders, errorReporter, errorReporter, DefinitionRequester.INSTANCE);
  }

  private @NotNull BuildVisitor buildVisitor() {
    return new BuildVisitor(Repl.replModulePath, myErrorReporter);
  }

  public static @NotNull ArendParser createParser(@NotNull String text, @NotNull ModuleLocation moduleLocation, @NotNull ErrorReporter reporter) {
    var errorListener = new ReporterErrorListener(reporter, moduleLocation.getModulePath());
    var parser = new ArendParser(
        new CommonTokenStream(createLexer(text, errorListener)));
    parser.removeErrorListeners();
    parser.addErrorListener(errorListener);
    // parser.addErrorListener(new DiagnosticErrorListener());
    // parser.getInterpreter().setPredictionMode(PredictionMode.LL_EXACT_AMBIG_DETECTION);
    return parser;
  }

  public static @NotNull ArendLexer createLexer(@NotNull String text, ReporterErrorListener errorListener) {
    var input = CharStreams.fromString(text);
    var lexer = new ArendLexer(input);
    lexer.removeErrorListeners();
    lexer.addErrorListener(errorListener);
    return lexer;
  }

  private @NotNull ArendParser parse(String line) {
    return createParser(line, Repl.replModulePath, myErrorReporter);
  }

  @Override
  public @NotNull String prompt() {
    return prompt;
  }

  @Override
  protected void loadCommands() {
    super.loadCommands();
    registerAction("modules", ListLoadedModulesAction.INSTANCE);
    registerAction("unload", UnloadModuleCommand.INSTANCE);
    registerAction("load", LoadModuleCommand.INSTANCE);
    registerAction("reload", LoadModuleCommand.ReloadModuleCommand.INSTANCE);
    registerAction("prompt", new ChangePromptCommand());
    registerAction("lib", LoadLibraryCommand.INSTANCE);
    registerAction("pwd", new PwdCommand());
    registerAction("cd", new CdCommand());
  }

  public @Nullable Library createLibrary(@NotNull String path) {
    var library = myLibraryResolver.resolve(myReplLibrary, path);
    if (library != null) return library;
    return myLibraryResolver.registerLibrary(pwd.resolve(path).toAbsolutePath().normalize());
  }

  public final void addLibraryDirectories(@NotNull Collection<? extends Path> libDirs) {
    myLibraryResolver.addLibraryDirectories(libDirs);
  }

  @Override
  protected final @Nullable FileGroup parseStatements(@NotNull String line) {
    var fileGroup = buildVisitor().visitStatements(parse(line).statements());
    if (fileGroup != null)
      fileGroup.setModuleScopeProvider(getAvailableModuleScopeProvider());
    if (checkErrors()) return null;
    return fileGroup;
  }

  @Override
  protected final @Nullable Concrete.Expression parseExpr(@NotNull String text) {
    return buildVisitor().visitExpr(parse(text).expr());
  }

  public CommonCliRepl() {
    this(new SimpleTypecheckerState(), new TreeSet<>(), new ListErrorReporter(new ArrayList<>()));
  }

  /**
   * @return false means type checking failed.
   */
  public final boolean loadLibrary(@NotNull Library library) {
    if (!myLibraryManager.loadLibrary(library, myTypechecking)) return false;
    myLibraryManager.registerDependency(myReplLibrary, library);
    typecheckLibrary(library);
    return true;
  }

  @Override
  protected final void loadLibraries() {
    if (!loadLibrary(new PreludeResourceLibrary(myTypecheckerState)))
      eprintln("[FATAL] Failed to load Prelude");
    else myReplScope.addPreludeScope(PreludeLibrary.getPreludeScope());
    if (!myLibraryManager.loadLibrary(myReplLibrary, myTypechecking))
      eprintln("[FATAL] Failed to load the REPL virtual library");
    typecheckLibrary(myReplLibrary);
  }

  /**
   * Load a file under the REPL working directory and get its scope.
   * This will <strong>not</strong> modify the REPL scope.
   */
  public final @Nullable Scope loadModule(@NotNull ModulePath modulePath) {
    if (myModules.add(modulePath))
      myLibraryManager.unloadLibrary(myReplLibrary);
    myLibraryManager.loadLibrary(myReplLibrary, myTypechecking);
    typecheckLibrary(myReplLibrary);
    return getAvailableModuleScopeProvider().forModule(modulePath);
  }

  /**
   * Like {@link CommonCliRepl#loadModule(ModulePath)}, this will
   * <strong>not</strong> modify the REPL scope as well.
   *
   * @return true if the module is already loaded before.
   */
  public final boolean unloadModule(@NotNull ModulePath modulePath) {
    boolean isLoadedBefore = myModules.remove(modulePath);
    if (isLoadedBefore) {
      myLibraryManager.unloadLibrary(myReplLibrary);
      myReplLibrary.groupLoaded(modulePath, null, true, false);
      typecheckLibrary(myReplLibrary);
    }
    return isLoadedBefore;
  }

  public @NotNull SourceLibrary getReplLibrary() {
    return myReplLibrary;
  }

  private final class ChangePromptCommand implements ReplCommand {
    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String description() {
      return "Change the repl prompt (current prompt: '" + prompt + "')";
    }

    @Override
    public void invoke(@NotNull String line, @NotNull Repl api, @NotNull Supplier<@NotNull String> scanner) {
      boolean start = line.startsWith("\"");
      boolean end = line.endsWith("\"");
      // Maybe we should unescape this string?
      if (start && end) prompt = line.substring(1, line.length() - 1);
      else if (!start && !end) prompt = line;
      else eprintln("[ERROR] Bad prompt format");
    }
  }
}
