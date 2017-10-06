package com.jetbrains.jetpad.vclang.frontend.parser;

import com.jetbrains.jetpad.vclang.error.CompositeErrorReporter;
import com.jetbrains.jetpad.vclang.error.CountingErrorReporter;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.ReferenceConcreteProvider;
import com.jetbrains.jetpad.vclang.frontend.namespace.ModuleRegistry;
import com.jetbrains.jetpad.vclang.frontend.namespace.SimpleDynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.frontend.namespace.SimpleStaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.resolving.GroupNameResolver;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Group;
import org.antlr.v4.runtime.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;

public abstract class ParseSource {
  private final SourceId mySourceId;
  private final Reader myStream;

  public ParseSource(SourceId sourceId, Reader stream) {
    mySourceId = sourceId;
    myStream = stream;
  }

  public @Nullable
  Group load(ErrorReporter errorReporter, ModuleRegistry moduleRegistry, Scope globalScope, NameResolver nameResolver) throws IOException {
    CountingErrorReporter countingErrorReporter = new CountingErrorReporter();
    final CompositeErrorReporter compositeErrorReporter = new CompositeErrorReporter(errorReporter, countingErrorReporter);

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

    Group result = new BuildVisitor(mySourceId, errorReporter).visitStatements(tree);

    if (moduleRegistry != null) {
      moduleRegistry.registerModule(mySourceId.getModulePath(), result);
    }
    if (nameResolver != null) {
      if (nameResolver.nsProviders.statics instanceof SimpleStaticNamespaceProvider) { // TODO[abstract]: Move this somewhere else, like BaseCliFrontend or whatever
        ((SimpleStaticNamespaceProvider) nameResolver.nsProviders.statics).collect(result, errorReporter);
      }
      if (nameResolver.nsProviders.dynamics instanceof SimpleDynamicNamespaceProvider) {
        ((SimpleDynamicNamespaceProvider) nameResolver.nsProviders.dynamics).collect(result, errorReporter, nameResolver);
      }
      new GroupNameResolver(nameResolver, errorReporter, ReferenceConcreteProvider.INSTANCE).resolveGroup(result, globalScope);
    }
    return result;
  }
}
