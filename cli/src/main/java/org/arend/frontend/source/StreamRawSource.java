package org.arend.frontend.source;

import org.antlr.v4.runtime.*;
import org.arend.error.CountingErrorReporter;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.module.ModulePath;
import org.arend.frontend.ConcreteReferableProvider;
import org.arend.frontend.parser.*;
import org.arend.library.SourceLibrary;
import org.arend.module.ModuleLocation;
import org.arend.module.error.ExceptionError;
import org.arend.naming.resolving.visitor.DefinitionResolveNameVisitor;
import org.arend.naming.scope.CachingScope;
import org.arend.naming.scope.ScopeFactory;
import org.arend.source.Source;
import org.arend.source.SourceLoader;
import org.arend.term.NamespaceCommand;
import org.arend.term.group.FileGroup;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a source that loads a raw module from an {@link InputStream}.
 */
public abstract class StreamRawSource implements Source {
  private final ModulePath myModulePath;
  private final boolean myInTests;
  private FileGroup myGroup;
  private byte myPass = 0;

  protected StreamRawSource(ModulePath modulePath, boolean inTests) {
    myModulePath = modulePath;
    myInTests = inTests;
  }

  @NotNull
  @Override
  public ModulePath getModulePath() {
    return myModulePath;
  }

  /**
   * Gets an input stream from which the source will be loaded.
   *
   * @return an input stream from which the source will be loaded or null if some error occurred.
   */
  @NotNull
  protected abstract InputStream getInputStream() throws IOException;

  @Override
  public boolean preload(SourceLoader sourceLoader) {
    SourceLibrary library = sourceLoader.getLibrary();
    ModulePath modulePath = getModulePath();
    ErrorReporter errorReporter = sourceLoader.getTypecheckingErrorReporter();
    CountingErrorReporter countingErrorReporter = new CountingErrorReporter(errorReporter);

    try {
      var errorListener = new ReporterErrorListener(countingErrorReporter, modulePath);

      ArendLexer lexer = new ArendLexer(CharStreams.fromStream(getInputStream()));
      lexer.removeErrorListeners();
      lexer.addErrorListener(errorListener);

      ArendParser parser = new ArendParser(new CommonTokenStream(lexer));
      parser.removeErrorListeners();
      parser.addErrorListener(errorListener);

      ArendParser.StatementsContext tree = parser.statements();
      if (countingErrorReporter.getErrorsNumber() > 0) {
        return false;
      }

      myGroup = new BuildVisitor(new ModuleLocation(library.getName(), myInTests ? ModuleLocation.LocationKind.TEST : ModuleLocation.LocationKind.SOURCE, modulePath), errorReporter).visitStatements(tree);
      library.groupLoaded(modulePath, myGroup, true, myInTests);

      for (NamespaceCommand command : myGroup.getNamespaceCommands()) {
        if (command.getKind() == NamespaceCommand.Kind.IMPORT) {
          ModulePath module = new ModulePath(command.getPath());
          if (library.containsModule(module) && !sourceLoader.preloadRaw(module, myInTests)) {
            library.groupLoaded(modulePath, null, true, myInTests);
            myGroup = null;
            return false;
          }
        }
      }

      return true;
    } catch (IOException e) {
      errorReporter.report(new ExceptionError(e, "loading", modulePath));
      library.groupLoaded(modulePath, null, true, myInTests);
      return false;
    }
  }

  @Override
  public LoadResult load(SourceLoader sourceLoader) {
    if (myGroup == null) {
      return LoadResult.FAIL;
    }

    if (myPass == 0) {
      myGroup.setModuleScopeProvider(sourceLoader.getModuleScopeProvider(myInTests));
      myPass = 1;
      return LoadResult.CONTINUE;
    }

    new DefinitionResolveNameVisitor(ConcreteReferableProvider.INSTANCE, myPass == 1, sourceLoader.getTypecheckingErrorReporter()).resolveGroup(myGroup, null, myGroup.getGroupScope());
    if (myPass == 1) {
      myPass = 2;
      return LoadResult.CONTINUE;
    }
    sourceLoader.getInstanceProviderSet().collectInstances(myGroup, CachingScope.make(ScopeFactory.parentScopeForGroup(myGroup, sourceLoader.getModuleScopeProvider(myInTests), true)), ConcreteReferableProvider.INSTANCE, null);
    return LoadResult.SUCCESS;
  }
}
