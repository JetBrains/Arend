package com.jetbrains.jetpad.vclang.module.source.file;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.parser.BuildVisitor;
import com.jetbrains.jetpad.vclang.parser.ParserError;
import com.jetbrains.jetpad.vclang.parser.VcgrammarLexer;
import com.jetbrains.jetpad.vclang.parser.VcgrammarParser;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import org.antlr.v4.runtime.*;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

public abstract class ParseSource {
  private final SourceId mySourceId;
  private final Reader myStream;

  public ParseSource(SourceId sourceId, Reader stream) {
    mySourceId = sourceId;
    myStream = stream;
  }

  protected SourceId getId() {
    return mySourceId;
  }

  public Abstract.ClassDefinition load(final ErrorReporter errorReporter) throws IOException {
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
    if (tree == null) {
      return null;
    }

    List<Concrete.Statement> statements = new BuildVisitor(mySourceId, errorReporter).visitStatements(tree);
    return new Concrete.ClassDefinition(new Concrete.Position(mySourceId, 0, 0), mySourceId.getModulePath().getName(), statements);
  }
}
