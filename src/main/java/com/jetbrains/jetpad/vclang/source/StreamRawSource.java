package com.jetbrains.jetpad.vclang.source;

import com.jetbrains.jetpad.vclang.error.CompositeErrorReporter;
import com.jetbrains.jetpad.vclang.error.CountingErrorReporter;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.ConcreteReferableProvider;
import com.jetbrains.jetpad.vclang.frontend.parser.*;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.error.ExceptionError;
import com.jetbrains.jetpad.vclang.naming.resolving.visitor.DefinitionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.term.NamespaceCommand;
import com.jetbrains.jetpad.vclang.term.group.FileGroup;
import org.antlr.v4.runtime.*;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a source that loads a raw module from an {@link InputStream}.
 */
public abstract class StreamRawSource implements Source {
  /**
   * Gets an input stream from which the source will be loaded.
   *
   * @return an input stream from which the source will be loaded or null if some error occurred.
   */
  @Nonnull
  protected abstract InputStream getInputStream() throws IOException;

  @Override
  public boolean load(SourceLoader sourceLoader) {
    ModulePath modulePath = getModulePath();
    ErrorReporter errorReporter = sourceLoader.getErrorReporter();
    CountingErrorReporter countingErrorReporter = new CountingErrorReporter();
    final CompositeErrorReporter compositeErrorReporter = new CompositeErrorReporter(errorReporter, countingErrorReporter);

    try {
      VcgrammarLexer lexer = new VcgrammarLexer(new ANTLRInputStream(getInputStream()));
      lexer.removeErrorListeners();
      lexer.addErrorListener(new BaseErrorListener() {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
          compositeErrorReporter.report(new ParserError(new Position(modulePath, line, pos), msg));
        }
      });

      VcgrammarParser parser = new VcgrammarParser(new CommonTokenStream(lexer));
      parser.removeErrorListeners();
      parser.addErrorListener(new BaseErrorListener() {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
          compositeErrorReporter.report(new ParserError(new Position(modulePath, line, pos), msg));
        }
      });

      VcgrammarParser.StatementsContext tree = parser.statements();
      if (tree == null || countingErrorReporter.getErrorsNumber() > 0) {
        sourceLoader.getLibrary().onModuleLoaded(modulePath, null, true);
        return false;
      }

      FileGroup result = new BuildVisitor(modulePath, errorReporter).visitStatements(tree);
      sourceLoader.getLibrary().onModuleLoaded(modulePath, result, true);
      result.setModuleScopeProvider(sourceLoader.getModuleScopeProvider());

      for (NamespaceCommand command : result.getNamespaceCommands()) {
        if (command.getKind() == NamespaceCommand.Kind.IMPORT) {
          sourceLoader.load(new ModulePath(command.getPath()));
        }
      }

      new DefinitionResolveNameVisitor(errorReporter).resolveGroup(result, result.getGroupScope(), ConcreteReferableProvider.INSTANCE);
      return true;
    } catch (IOException e) {
      errorReporter.report(new ExceptionError(e, modulePath));
      sourceLoader.getLibrary().onModuleLoaded(modulePath, null, true);
      return false;
    }
  }
}
