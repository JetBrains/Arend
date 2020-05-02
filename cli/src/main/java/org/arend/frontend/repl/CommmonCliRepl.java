package org.arend.frontend.repl;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.arend.error.ListErrorReporter;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.module.ModulePath;
import org.arend.frontend.ConcreteReferableProvider;
import org.arend.frontend.FileLibraryResolver;
import org.arend.frontend.PositionComparator;
import org.arend.frontend.library.FileSourceLibrary;
import org.arend.frontend.parser.ArendLexer;
import org.arend.frontend.parser.ArendParser;
import org.arend.frontend.parser.BuildVisitor;
import org.arend.frontend.parser.ReporterErrorListener;
import org.arend.library.Library;
import org.arend.library.SourceLibrary;
import org.arend.naming.scope.Scope;
import org.arend.prelude.PreludeLibrary;
import org.arend.prelude.PreludeResourceLibrary;
import org.arend.repl.Repl;
import org.arend.frontend.repl.action.LoadLibraryCommand;
import org.arend.frontend.repl.action.LoadModuleCommand;
import org.arend.repl.action.ReplCommand;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.FileGroup;
import org.arend.typechecking.SimpleTypecheckerState;
import org.arend.util.Range;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

public abstract class CommmonCliRepl extends Repl {
  public static final @NotNull String APP_NAME = "Arend REPL";

  private @NotNull String prompt = "\u03bb ";
  private final FileLibraryResolver myLibraryResolver;
  private final SourceLibrary myReplLibrary;
  private final Set<ModulePath> myModules;

  //region Tricky constructors (expand to read more...)
  // These two constructors are used for convincing javac that the
  // initialization is of well order.
  // All of the parameters introduced here are used more than once,
  // and one cannot introduce them as variable before the `this` or
  // `super` call because that's the rule of javac.
  private CommmonCliRepl(
      @NotNull SimpleTypecheckerState typecheckerState,
      @NotNull Set<ModulePath> modules,
      @NotNull ListErrorReporter errorReporter) {
    this(typecheckerState, modules, new FileLibraryResolver(new ArrayList<>(), typecheckerState, errorReporter), errorReporter);
  }

  private CommmonCliRepl(
      @NotNull SimpleTypecheckerState typecheckerState,
      @NotNull Set<ModulePath> modules,
      @NotNull FileLibraryResolver libraryResolver,
      @NotNull ListErrorReporter errorReporter) {
    super(
        errorReporter,
        libraryResolver,
        ConcreteReferableProvider.INSTANCE,
        PositionComparator.INSTANCE,
        typecheckerState
    );
    myLibraryResolver = libraryResolver;
    myReplLibrary = new FileSourceLibrary("Repl", Paths.get("."), null, null, null, modules, true, new ArrayList<>(), Range.unbound(), typecheckerState);
    myModules = modules;
  }
  //endregion

  private @NotNull BuildVisitor buildVisitor() {
    return new BuildVisitor(Repl.replModulePath, myErrorReporter);
  }

  public static @NotNull ArendParser createParser(@NotNull String text, @NotNull ModulePath modulePath, @NotNull ErrorReporter reporter) {
    var errorListener = new ReporterErrorListener(reporter, modulePath);
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

  @Override
  public void eprintln(Object anything) {
    System.err.println(anything);
  }

  @Override
  public void println(Object anything) {
    System.out.println(anything);
  }

  @Override
  public void print(Object anything) {
    System.out.print(anything);
    System.out.flush();
  }

  @Override
  public void printlnOpt(Object anything, boolean toError) {
    (toError ? System.err : System.out).println(anything);
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
    registerAction("load", LoadModuleCommand.INSTANCE);
    registerAction("l", LoadModuleCommand.INSTANCE);
    registerAction("reload", LoadModuleCommand.ReloadModuleCommand.INSTANCE);
    registerAction("r", LoadModuleCommand.ReloadModuleCommand.INSTANCE);
    registerAction("prompt", new ChangePromptCommand());
    registerAction("lib", LoadLibraryCommand.INSTANCE);
  }

  public  @Nullable Library createLibrary(@NotNull String path) {
    return myLibraryResolver.registerLibrary(Paths.get(path).toAbsolutePath());
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

  public CommmonCliRepl() {
    this(new SimpleTypecheckerState(), new TreeSet<>(), new ListErrorReporter(new ArrayList<>()));
  }

  public final boolean loadLibrary(@NotNull Library library) {
    if (!myLibraryManager.loadLibrary(library, myTypechecking)) return false;
    myLibraryManager.registerDependency(myReplLibrary, library);
    return true;
  }

  @Override
  protected final void loadLibraries() {
    if (!loadLibrary(new PreludeResourceLibrary(myTypecheckerState)))
      eprintln("[FATAL] Failed to load Prelude");
    else myMergedScopes.add(PreludeLibrary.getPreludeScope());
    if (!myLibraryManager.loadLibrary(myReplLibrary, myTypechecking))
      eprintln("[FATAL] Failed to load the REPL virtual library");
  }

  /**
   * Load a file under the REPL working directory and get its scope.
   * This will <strong>not</strong> modify the REPL scope.
   */
  public final @Nullable Scope loadModule(@NotNull ModulePath modulePath) {
    boolean isLoadedBefore = myModules.add(modulePath);
    myLibraryManager.reload(myTypechecking);
    if (checkErrors()) {
      myModules.remove(modulePath);
      return null;
    }
    if (isLoadedBefore) {
      Scope scope = getAvailableModuleScopeProvider().forModule(modulePath);
      if (scope != null) removeScope(scope);
    }
    typecheckLibrary(myReplLibrary);
    return getAvailableModuleScopeProvider().forModule(modulePath);
  }

  /**
   * Like {@link CommmonCliRepl#loadModule(ModulePath)}, this will
   * <strong>not</strong> modify the REPL scope as well.
   *
   * @return true if the module is already loaded before.
   */
  public final boolean unloadModule(@NotNull ModulePath modulePath) {
    boolean isLoadedBefore = myModules.remove(modulePath);
    if (isLoadedBefore) {
      myLibraryManager.reload(myTypechecking);
      Scope scope = getAvailableModuleScopeProvider().forModule(modulePath);
      if (scope != null) removeScope(scope);
      myReplLibrary.onGroupLoaded(modulePath, null, true);
      typecheckLibrary(myReplLibrary);
    }
    return isLoadedBefore;
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
