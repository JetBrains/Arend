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
import org.arend.repl.Repl;
import org.arend.repl.action.LoadLibraryCommand;
import org.arend.repl.action.ReplCommand;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.FileGroup;
import org.arend.typechecking.SimpleTypecheckerState;
import org.arend.util.Range;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

public abstract class CommmonCliRepl extends Repl {
  public static final @NotNull String APP_NAME = "Arend REPL";

  private @NotNull String prompt = "\u03bb ";
  private final FileLibraryResolver myLibraryResolver;

  //region Tricky constructors (expand to read more...)
  // These two constructors are used for convincing javac that the
  // initialization is of well order.
  // All of the parameters introduced here are used more than once,
  // and one cannot introduce them as variable before the `this` or
  // `super` call because that's the rule of javac.
  private CommmonCliRepl(
      @NotNull SimpleTypecheckerState typecheckerState,
      @NotNull Set<ModulePath> modules,
      @NotNull PrintStream stdout,
      @NotNull ListErrorReporter errorReporter) {
    this(typecheckerState, modules, new FileLibraryResolver(new ArrayList<>(), typecheckerState, errorReporter), stdout, errorReporter);
  }

  private CommmonCliRepl(
      @NotNull SimpleTypecheckerState typecheckerState,
      @NotNull Set<ModulePath> modules,
      @NotNull FileLibraryResolver libraryResolver,
      @NotNull PrintStream stdout,
      @NotNull ListErrorReporter errorReporter) {
    super(
        errorReporter,
        libraryResolver,
        ConcreteReferableProvider.INSTANCE,
        PositionComparator.INSTANCE,
        stdout, System.err,
        modules,
        new FileSourceLibrary("Repl", Paths.get("."), null, null, null, modules, true, new ArrayList<>(), Range.unbound(), typecheckerState),
        typecheckerState
    );
    myLibraryResolver = libraryResolver;
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
    registerAction("prompt", new ChangePromptCommand());
    registerAction("lib", new LoadLibraryCommand() {
      @Override
      protected @Nullable Library createLibrary(@NotNull String path) {
        return myLibraryResolver.registerLibrary(Paths.get(path).toAbsolutePath());
      }
    });
  }

  @Override
  protected final @Nullable FileGroup parseStatements(String line) {
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

  public CommmonCliRepl(@NotNull PrintStream stdout) {
    this(new SimpleTypecheckerState(), new TreeSet<>(), stdout, new ListErrorReporter(new ArrayList<>()));
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
