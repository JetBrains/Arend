package com.jetbrains.jetpad.vclang.frontend.parser;

import com.jetbrains.jetpad.vclang.error.CompositeErrorReporter;
import com.jetbrains.jetpad.vclang.error.CountingErrorReporter;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.ConcreteReferableProvider;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.ModuleRegistry;
import com.jetbrains.jetpad.vclang.module.ModuleResolver;
import com.jetbrains.jetpad.vclang.module.SimpleModuleScopeProvider;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.naming.resolving.visitor.DefinitionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.scope.CachingScope;
import com.jetbrains.jetpad.vclang.naming.scope.ScopeFactory;
import com.jetbrains.jetpad.vclang.term.ChildGroup;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.NamespaceCommand;
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
  Group load(ErrorReporter errorReporter, @Nullable ModuleRegistry moduleRegistry, @Nullable ModuleResolver moduleResolver) throws IOException {
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

    ChildGroup result = new BuildVisitor(mySourceId, errorReporter).visitStatements(tree);

    if (moduleRegistry != null) {
      moduleRegistry.registerModule(mySourceId.getModulePath(), result);
    }

    if (moduleResolver != null) {
      for (NamespaceCommand command : result.getNamespaceCommands()) {
        if (command.getKind() == NamespaceCommand.Kind.IMPORT) {
          moduleResolver.load(new ModulePath(command.getPath()));
        }
      }
    }

    new DefinitionResolveNameVisitor(errorReporter).resolveGroup(result, new CachingScope(ScopeFactory.forGroup(result, SimpleModuleScopeProvider.INSTANCE)), ConcreteReferableProvider.INSTANCE);
    return result;
  }
}
