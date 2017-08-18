package com.jetbrains.jetpad.vclang.frontend.text.parser;

import com.jetbrains.jetpad.vclang.error.CompositeErrorReporter;
import com.jetbrains.jetpad.vclang.error.CountingErrorReporter;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.ConcretePrettyPrinterInfoProvider;
import com.jetbrains.jetpad.vclang.frontend.ConcreteResolveListener;
import com.jetbrains.jetpad.vclang.frontend.namespace.ModuleRegistry;
import com.jetbrains.jetpad.vclang.frontend.resolving.OneshotNameResolver;
import com.jetbrains.jetpad.vclang.frontend.text.Position;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;
import com.jetbrains.jetpad.vclang.term.Concrete;
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

  public @Nullable Concrete.ClassDefinition load(ErrorReporter<Position> errorReporter, ModuleRegistry moduleRegistry, Scope globalScope, NameResolver nameResolver) throws IOException {
    CountingErrorReporter<Position> countingErrorReporter = new CountingErrorReporter<>();
    final CompositeErrorReporter<Position> compositeErrorReporter = new CompositeErrorReporter<>(errorReporter, countingErrorReporter);

    VcgrammarLexer lexer = new VcgrammarLexer(new ANTLRInputStream(myStream));
    lexer.removeErrorListeners();
    lexer.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        compositeErrorReporter.report(new ParserError(new Position(mySourceId, line, pos), msg));
      }
    });

    VcgrammarParser parser = new VcgrammarParser(new CommonTokenStream(lexer));
    parser.removeErrorListeners();
    parser.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        compositeErrorReporter.report(new ParserError(new Position(mySourceId, line, pos), msg));
      }
    });

    VcgrammarParser.StatementsContext tree = parser.statements();
    if (tree == null || countingErrorReporter.getErrorsNumber() > 0) {
      return null;
    }

    List<Concrete.Statement<Position>> statements = new BuildVisitor(mySourceId, compositeErrorReporter).visitStatements(tree);

    Concrete.ClassDefinition<Position> result = new Concrete.ClassDefinition<>(new Position(mySourceId, 0, 0), mySourceId.getModulePath().getName(), statements);

    if (moduleRegistry != null) {
      moduleRegistry.registerModule(mySourceId.getModulePath(), result);
    }
    if (nameResolver != null) {
      OneshotNameResolver.visitModule(result, globalScope, nameResolver, ConcretePrettyPrinterInfoProvider.INSTANCE, new ConcreteResolveListener(), compositeErrorReporter);
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
