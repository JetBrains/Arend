package org.arend.source;

import org.antlr.v4.runtime.*;
import org.arend.error.CompositeErrorReporter;
import org.arend.error.CountingErrorReporter;
import org.arend.error.ErrorReporter;
import org.arend.frontend.ConcreteReferableProvider;
import org.arend.frontend.parser.*;
import org.arend.library.SourceLibrary;
import org.arend.module.ModulePath;
import org.arend.module.error.ExceptionError;
import org.arend.naming.resolving.visitor.DefinitionResolveNameVisitor;
import org.arend.naming.scope.CachingScope;
import org.arend.naming.scope.ScopeFactory;
import org.arend.term.NamespaceCommand;
import org.arend.term.group.FileGroup;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a source that loads a raw module from an {@link InputStream}.
 */
public abstract class StreamRawSource implements Source {
  private final ModulePath myModulePath;
  private FileGroup myGroup;
  private boolean myFirstPass = true;

  protected StreamRawSource(ModulePath modulePath) {
    myModulePath = modulePath;
  }

  @Nonnull
  @Override
  public ModulePath getModulePath() {
    return myModulePath;
  }

  /**
   * Gets an input stream from which the source will be loaded.
   *
   * @return an input stream from which the source will be loaded or null if some error occurred.
   */
  @Nonnull
  protected abstract InputStream getInputStream() throws IOException;

  @Override
  public boolean preload(SourceLoader sourceLoader) {
    SourceLibrary library = sourceLoader.getLibrary();
    ModulePath modulePath = getModulePath();
    ErrorReporter errorReporter = sourceLoader.getTypecheckingErrorReporter();
    CountingErrorReporter countingErrorReporter = new CountingErrorReporter();
    final CompositeErrorReporter compositeErrorReporter = new CompositeErrorReporter(errorReporter, countingErrorReporter);

    try {
      BaseErrorListener errorListener = new BaseErrorListener() {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
          compositeErrorReporter.report(new ParserError(new Position(modulePath, line, pos), msg));
        }
      };

      ArendLexer lexer = new ArendLexer(new ANTLRInputStream(getInputStream()));
      lexer.removeErrorListeners();
      lexer.addErrorListener(errorListener);

      ArendParser parser = new ArendParser(new CommonTokenStream(lexer));
      parser.removeErrorListeners();
      parser.addErrorListener(errorListener);

      ArendParser.StatementsContext tree = parser.statements();
      if (tree == null || countingErrorReporter.getErrorsNumber() > 0) {
        return false;
      }

      myGroup = new BuildVisitor(modulePath, errorReporter).visitStatements(tree);
      library.onGroupLoaded(modulePath, myGroup, true);

      for (NamespaceCommand command : myGroup.getNamespaceCommands()) {
        if (command.getKind() == NamespaceCommand.Kind.IMPORT) {
          ModulePath module = new ModulePath(command.getPath());
          if (library.containsModule(module) && !sourceLoader.preloadRaw(module)) {
            library.onGroupLoaded(modulePath, null, true);
            myGroup = null;
            return false;
          }
        }
      }

      myGroup.setModuleScopeProvider(sourceLoader.getModuleScopeProvider());
      return true;
    } catch (IOException e) {
      errorReporter.report(new ExceptionError(e, modulePath, true));
      library.onGroupLoaded(modulePath, null, true);
      return false;
    }
  }

  @Override
  public LoadResult load(SourceLoader sourceLoader) {
    if (myGroup == null) {
      return LoadResult.FAIL;
    }

    new DefinitionResolveNameVisitor(ConcreteReferableProvider.INSTANCE, myFirstPass, sourceLoader.getTypecheckingErrorReporter()).resolveGroup(myGroup, null, myGroup.getGroupScope());
    if (myFirstPass) {
      myFirstPass = false;
      return LoadResult.CONTINUE;
    }
    sourceLoader.getInstanceProviderSet().collectInstances(myGroup, CachingScope.make(ScopeFactory.parentScopeForGroup(myGroup, sourceLoader.getModuleScopeProvider(), true)), ConcreteReferableProvider.INSTANCE, null);
    return LoadResult.SUCCESS;
  }
}
