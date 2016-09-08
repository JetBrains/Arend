package com.jetbrains.jetpad.vclang.module.source.file;

import com.jetbrains.jetpad.vclang.error.CompositeErrorReporter;
import com.jetbrains.jetpad.vclang.error.CountingErrorReporter;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.source.ModuleSourceId;
import com.jetbrains.jetpad.vclang.module.source.Source;
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

public abstract class ParseSource implements Source {
  private final ErrorReporter myErrorReporter;
  private final ModuleSourceId mySourceId;
  private final InputStream myStream;

  public ParseSource(ModuleSourceId sourceId, InputStream stream, ErrorReporter errorReporter) {
    mySourceId = sourceId;
    myStream = stream;
    myErrorReporter = errorReporter;
  }

  protected ModuleSourceId getId() {
    return mySourceId;
  }

  public InputStream getStream() {
    return myStream;
  }

  public ModuleLoader.Result load() throws IOException {
    CountingErrorReporter countingErrorReporter = new CountingErrorReporter(GeneralError.Level.ERROR);
    final CompositeErrorReporter errorReporter = new CompositeErrorReporter(myErrorReporter, countingErrorReporter);

    VcgrammarLexer lexer = new VcgrammarLexer(new ANTLRInputStream(myStream));
    lexer.removeErrorListeners();
    lexer.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        errorReporter.report(new ParserError(mySourceId, new Concrete.Position(mySourceId, line, pos), msg));
      }
    });

    VcgrammarParser parser = new VcgrammarParser(new CommonTokenStream(lexer));
    parser.removeErrorListeners();
    parser.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        errorReporter.report(new ParserError(mySourceId, new Concrete.Position(mySourceId, line, pos), msg));
      }
    });

    VcgrammarParser.StatementsContext tree = parser.statements();
    if (tree == null || countingErrorReporter.getErrorsNumber() != 0) {
      return new ModuleLoader.Result(null, countingErrorReporter.getErrorsNumber());
    }

    List<Concrete.Statement> statements = new BuildVisitor(mySourceId, errorReporter).visitStatements(tree);
    Concrete.ClassDefinition classDefinition = new Concrete.ClassDefinition(new Concrete.Position(mySourceId, 0, 0), mySourceId.getModulePath().getName(), statements, Abstract.ClassDefinition.Kind.Module);
    for (Concrete.Statement statement : statements) {
      if (statement instanceof Concrete.DefineStatement) {
        ((Concrete.DefineStatement) statement).setParentDefinition(classDefinition);
      }
    }

    return new ModuleLoader.Result(classDefinition, countingErrorReporter.getErrorsNumber());
  }
}
