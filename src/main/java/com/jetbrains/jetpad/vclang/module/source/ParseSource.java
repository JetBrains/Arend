package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.ModuleLoadingResult;
import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.parser.BuildVisitor;
import com.jetbrains.jetpad.vclang.parser.ParserError;
import com.jetbrains.jetpad.vclang.parser.VcgrammarLexer;
import com.jetbrains.jetpad.vclang.parser.VcgrammarParser;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.typechecking.error.CompositeErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.CountingErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.DeepNamespaceNameResolver;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.LoadingNameResolver;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.NameResolver;
import org.antlr.v4.runtime.*;

import java.io.IOException;
import java.io.InputStream;

public abstract class ParseSource implements Source {
  private final ModuleLoader myModuleLoader;
  private final ErrorReporter myErrorReporter;
  private final Namespace myModule;
  private InputStream myStream;

  public ParseSource(ModuleLoader moduleLoader, ErrorReporter errorReporter, Namespace module) {
    myModuleLoader = moduleLoader;
    myErrorReporter = errorReporter;
    myModule = module;
  }

  public InputStream getStream() {
    return myStream;
  }

  public void setStream(InputStream stream) {
    myStream = stream;
  }

  @Override
  public ModuleLoadingResult load(Namespace namespace) throws IOException {
    CountingErrorReporter countingErrorReporter = new CountingErrorReporter();
    final CompositeErrorReporter errorReporter = new CompositeErrorReporter();
    errorReporter.addErrorReporter(myErrorReporter);
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

    VcgrammarParser.DefsContext tree = parser.defs();
    if (tree == null || countingErrorReporter.getErrorsNumber() != 0) {
      return new ModuleLoadingResult(namespace, null, true, countingErrorReporter.getErrorsNumber());
    }

    NameResolver nameResolver = new LoadingNameResolver(myModuleLoader, new DeepNamespaceNameResolver(namespace.getParent()));
    ClassDefinition classDefinition = new ClassDefinition(namespace);
    new BuildVisitor(namespace, classDefinition.getLocalNamespace(), nameResolver, errorReporter).visitDefs(tree);
    return new ModuleLoadingResult(namespace, classDefinition, true, countingErrorReporter.getErrorsNumber());
  }
}
