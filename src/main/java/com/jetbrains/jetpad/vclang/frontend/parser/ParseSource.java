package com.jetbrains.jetpad.vclang.frontend.parser;

import com.jetbrains.jetpad.vclang.error.CompositeErrorReporter;
import com.jetbrains.jetpad.vclang.error.CountingErrorReporter;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.Concrete;
import com.jetbrains.jetpad.vclang.frontend.ConcreteResolveListener;
import com.jetbrains.jetpad.vclang.frontend.namespace.ModuleRegistry;
import com.jetbrains.jetpad.vclang.frontend.resolving.OneshotNameResolver;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;
import org.antlr.v4.runtime.*;

import javax.annotation.Nullable;
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

  public @Nullable Concrete.ClassDefinition load(ErrorReporter errorReporter, ModuleRegistry moduleRegistry, Scope globalScope, NameResolver nameResolver) throws IOException {
    CountingErrorReporter countingErrorReporter = new CountingErrorReporter();
    final CompositeErrorReporter compositeErrorReporter = new CompositeErrorReporter(errorReporter, countingErrorReporter);

    VcgrammarLexer lexer = new VcgrammarLexer(new ANTLRInputStream(myStream));
    lexer.removeErrorListeners();
    lexer.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        compositeErrorReporter.report(new ParserError(new Concrete.Position(mySourceId, line, pos), msg));
      }
    });

    VcgrammarParser parser = new VcgrammarParser(new CommonTokenStream(lexer));
    parser.removeErrorListeners();
    parser.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        compositeErrorReporter.report(new ParserError(new Concrete.Position(mySourceId, line, pos), msg));
      }
    });

    VcgrammarParser.StatementsContext tree = parser.statements();
    if (tree == null || countingErrorReporter.getErrorsNumber() > 0) {
      return null;
    }

    List<Concrete.Statement> statements = new BuildVisitor(mySourceId, compositeErrorReporter).visitStatements(tree);

    Concrete.ClassDefinition result = new Concrete.ClassDefinition(new Concrete.Position(mySourceId, 0, 0), mySourceId.getModulePath().getName(), statements);

    if (moduleRegistry != null) {
      moduleRegistry.registerModule(mySourceId.getModulePath(), result);
    }
    if (nameResolver != null) {
      OneshotNameResolver.visitModule(result, globalScope, nameResolver, Concrete.NamespaceCommandStatement.GET, new ConcreteResolveListener(), compositeErrorReporter);
    }
    if (countingErrorReporter.getErrorsNumber() > 0) {
      if (moduleRegistry != null) {
        moduleRegistry.unregisterModule(mySourceId.getModulePath());
      }
      return null;
    }
    return result;
  }
}
