package org.arend.frontend.repl;

import org.antlr.v4.runtime.*;
import org.arend.error.ListErrorReporter;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.module.ModulePath;
import org.arend.frontend.ConcreteReferableProvider;
import org.arend.frontend.FileLibraryResolver;
import org.arend.frontend.PositionComparator;
import org.arend.frontend.parser.*;
import org.arend.repl.ReplLibrary;
import org.arend.repl.ReplState;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.FileGroup;
import org.arend.typechecking.SimpleTypecheckerState;
import org.arend.typechecking.TypecheckerState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class CliReplState extends ReplState {
  public CliReplState(@NotNull TypecheckerState typecheckerState,
                      @NotNull ListErrorReporter errorReporter) {
    super(
        errorReporter,
        new FileLibraryResolver(new ArrayList<>(), typecheckerState, errorReporter),
        ConcreteReferableProvider.INSTANCE,
        PositionComparator.INSTANCE,
        System.out, System.err,
        typecheckerState
    );
  }

  private @NotNull BuildVisitor buildVisitor() {
    return new BuildVisitor(ReplLibrary.replModulePath, myErrorReporter);
  }

  public static @NotNull ArendParser createParser(@NotNull String text, @NotNull ModulePath modulePath, @NotNull ErrorReporter reporter) {
    var errorListener = new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        reporter.report(new ParserError(new Position(modulePath, line, pos), msg));
      }
    };
    var parser = new ArendParser(
        new CommonTokenStream(createLexer(text, errorListener)));
    parser.removeErrorListeners();
    parser.addErrorListener(errorListener);
    // parser.addErrorListener(new DiagnosticErrorListener());
    // parser.getInterpreter().setPredictionMode(PredictionMode.LL_EXACT_AMBIG_DETECTION);
    return parser;
  }

  public static @NotNull ArendLexer createLexer(@NotNull String text, BaseErrorListener errorListener) {
    var input = CharStreams.fromString(text);
    var lexer = new ArendLexer(input);
    lexer.removeErrorListeners();
    lexer.addErrorListener(errorListener);
    return lexer;
  }

  private @NotNull ArendParser parse(String line) {
    return createParser(line, ReplLibrary.replModulePath, myErrorReporter);
  }

  @Override
  protected final @Nullable FileGroup parseStatements(String line) {
    var fileGroup = buildVisitor().visitStatements(parse(line).statements());
    if (fileGroup != null) fileGroup.setModuleScopeProvider(myReplLibrary.getModuleScopeProvider());
    if (checkErrors()) return null;
    return fileGroup;
  }

  @Override
  protected final @Nullable Concrete.Expression parseExpr(@NotNull String text) {
    return buildVisitor().visitExpr(parse(text).expr());
  }

  public CliReplState() {
    this(new SimpleTypecheckerState(), new ListErrorReporter(new ArrayList<>()));
  }
}
