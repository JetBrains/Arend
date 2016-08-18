package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.error.CompositeErrorReporter;
import com.jetbrains.jetpad.vclang.error.CountingErrorReporter;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
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
  private final ModuleID myModule;
  private InputStream myStream;

  public ParseSource(ErrorReporter errorReporter, ModuleID module) {
    myErrorReporter = errorReporter;
    myModule = module;
  }

  protected ModuleID getModule() {
    return myModule;
  }

  public InputStream getStream() {
    return myStream;
  }

  public void setStream(InputStream stream) {
    myStream = stream;
  }

  public ModuleLoader.Result load() throws IOException {
    CountingErrorReporter countingErrorReporter = new CountingErrorReporter(GeneralError.Level.ERROR);
    final CompositeErrorReporter errorReporter = new CompositeErrorReporter(myErrorReporter, countingErrorReporter);

    VcgrammarLexer lexer = new VcgrammarLexer(new ANTLRInputStream(myStream));
    lexer.removeErrorListeners();
    lexer.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        errorReporter.report(new ParserError(myModule, new Concrete.Position(myModule, line, pos), msg));
      }
    });

    VcgrammarParser parser = new VcgrammarParser(new CommonTokenStream(lexer));
    parser.removeErrorListeners();
    parser.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        errorReporter.report(new ParserError(myModule, new Concrete.Position(myModule, line, pos), msg));
      }
    });

    VcgrammarParser.StatementsContext tree = parser.statements();
    if (tree == null || countingErrorReporter.getErrorsNumber() != 0) {
      return new ModuleLoader.Result(null, null, true, countingErrorReporter.getErrorsNumber());
    }

    List<Concrete.Statement> statements = new BuildVisitor(myModule, errorReporter).visitStatements(tree);
    Concrete.ClassDefinition classDefinition = new Concrete.ClassDefinition(new Concrete.Position(myModule, 0, 0), myModule.getModulePath().getName(), statements, Abstract.ClassDefinition.Kind.Module);
    classDefinition.setModuleID(myModule);
    for (Concrete.Statement statement : statements) {
      if (statement instanceof Concrete.DefineStatement) {
        ((Concrete.DefineStatement) statement).setParentDefinition(classDefinition);
      }
    }

    return new ModuleLoader.Result(classDefinition, null, true, countingErrorReporter.getErrorsNumber());
  }
}
