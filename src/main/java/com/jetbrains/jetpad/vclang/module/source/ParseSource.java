package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.ModuleLoadingResult;
import com.jetbrains.jetpad.vclang.parser.BuildVisitor;
import com.jetbrains.jetpad.vclang.parser.ParserError;
import com.jetbrains.jetpad.vclang.parser.VcgrammarLexer;
import com.jetbrains.jetpad.vclang.parser.VcgrammarParser;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.definition.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.CompositeErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.CountingErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.DeepNamespaceNameResolver;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.LoadingNameResolver;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.NameResolver;
import org.antlr.v4.runtime.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public abstract class ParseSource implements Source {
  private final ModuleLoader myModuleLoader;
  private final ErrorReporter myErrorReporter;
  private final ResolvedName myModule;
  private InputStream myStream;

  public ParseSource(ModuleLoader moduleLoader, ErrorReporter errorReporter, ResolvedName module) {
    myModuleLoader = moduleLoader;
    myErrorReporter = errorReporter;
    myModule = module;
  }

  ResolvedName getModule() {
    return myModule;
  }

  public InputStream getStream() {
    return myStream;
  }

  public void setStream(InputStream stream) {
    myStream = stream;
  }

  @Override
  public ModuleLoadingResult load() throws IOException {
    CountingErrorReporter countingErrorReporter = new CountingErrorReporter(GeneralError.Level.ERROR);
    final CompositeErrorReporter errorReporter = new CompositeErrorReporter();
    errorReporter.addErrorReporter(new LocalErrorReporter(myModule, myErrorReporter));
    errorReporter.addErrorReporter(countingErrorReporter);

    VcgrammarLexer lexer = new VcgrammarLexer(new ANTLRInputStream(myStream));
    lexer.removeErrorListeners();
    lexer.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        errorReporter.report(new ParserError(myModule, new Concrete.Position(line, pos), msg));
      }
    });

    VcgrammarParser parser = new VcgrammarParser(new CommonTokenStream(lexer));
    parser.removeErrorListeners();
    parser.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        errorReporter.report(new ParserError(myModule, new Concrete.Position(line, pos), msg));
      }
    });

    VcgrammarParser.StatementsContext tree = parser.statements();
    if (tree == null || countingErrorReporter.getErrorsNumber() != 0) {
      return new ModuleLoadingResult(null, true, countingErrorReporter.getErrorsNumber());
    }

    NameResolver nameResolver = new LoadingNameResolver(myModuleLoader, new DeepNamespaceNameResolver(myModule.namespace));
    List<Concrete.Statement> statements = new BuildVisitor(errorReporter).visitStatements(tree);
    Concrete.ClassDefinition classDefinition = new Concrete.ClassDefinition(new Concrete.Position(0, 0), myModule.name.name, statements);
    new DefinitionResolveNameVisitor(errorReporter, myModule.namespace, nameResolver).visitClass(classDefinition, null);
    return new ModuleLoadingResult(new NamespaceMember(myModule.namespace.getChild(myModule.name), classDefinition, null), true, countingErrorReporter.getErrorsNumber());
  }
}
