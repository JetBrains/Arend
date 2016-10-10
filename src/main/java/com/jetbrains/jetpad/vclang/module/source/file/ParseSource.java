package com.jetbrains.jetpad.vclang.module.source.file;

import com.jetbrains.jetpad.vclang.error.CompositeErrorReporter;
import com.jetbrains.jetpad.vclang.error.CountingErrorReporter;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.parser.BuildVisitor;
import com.jetbrains.jetpad.vclang.parser.ParserError;
import com.jetbrains.jetpad.vclang.parser.VcgrammarLexer;
import com.jetbrains.jetpad.vclang.parser.VcgrammarParser;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import org.antlr.v4.runtime.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public abstract class ParseSource {
  private final ErrorReporter myErrorReporter;
  private final SourceId mySourceId;
  private final InputStream myStream;

  public ParseSource(SourceId sourceId, InputStream stream, ErrorReporter errorReporter) {
    mySourceId = sourceId;
    myStream = stream;
    myErrorReporter = errorReporter;
  }

  protected SourceId getId() {
    return mySourceId;
  }

  public ParseSourceResult load() throws IOException {
    CountingErrorReporter countingErrorReporter = new CountingErrorReporter(GeneralError.Level.ERROR);
    final CompositeErrorReporter errorReporter = new CompositeErrorReporter(myErrorReporter, countingErrorReporter);

    VcgrammarLexer lexer = new VcgrammarLexer(new ANTLRInputStream(myStream));
    lexer.removeErrorListeners();
    lexer.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        errorReporter.report(new ParserError(new Concrete.Position(mySourceId, line, pos), msg));
      }
    });

    VcgrammarParser parser = new VcgrammarParser(new CommonTokenStream(lexer));
    parser.removeErrorListeners();
    parser.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        errorReporter.report(new ParserError(new Concrete.Position(mySourceId, line, pos), msg));
      }
    });

    VcgrammarParser.StatementsContext tree = parser.statements();
    if (tree == null || countingErrorReporter.getErrorsNumber() != 0) {
      return new ParseSourceResult(null, countingErrorReporter.getErrorsNumber());
    }

    List<Concrete.Statement> statements = new BuildVisitor(mySourceId, errorReporter).visitStatements(tree);
    Concrete.ClassDefinition classDefinition = new Concrete.ClassDefinition(new Concrete.Position(mySourceId, 0, 0), mySourceId.getModulePath().getName(), statements);
    return new ParseSourceResult(classDefinition, countingErrorReporter.getErrorsNumber());
  }

  public class ParseSourceResult {
    public final Abstract.ClassDefinition definition;
    public final int errorCount;

    private ParseSourceResult(Abstract.ClassDefinition definition, int errorCount) {
      this.definition = definition;
      this.errorCount = errorCount;
    }
  }
}
