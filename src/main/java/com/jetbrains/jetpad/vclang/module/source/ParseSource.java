package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.*;
import com.jetbrains.jetpad.vclang.naming.ModuleResolvedName;
import com.jetbrains.jetpad.vclang.parser.BuildVisitor;
import com.jetbrains.jetpad.vclang.parser.ParserError;
import com.jetbrains.jetpad.vclang.parser.VcgrammarLexer;
import com.jetbrains.jetpad.vclang.parser.VcgrammarParser;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionResolveStaticModVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.CompositeErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.CountingErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.DummyNameResolver;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.listener.ConcreteResolveListener;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.module.LoadingModuleResolver;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.module.ModuleResolver;
import org.antlr.v4.runtime.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public abstract class ParseSource implements Source {
  private final ModuleLoader myModuleLoader;
  private final ErrorReporter myErrorReporter;
  private final ModuleID myModule;
  private InputStream myStream;

  public ParseSource(ModuleLoader moduleLoader, ErrorReporter errorReporter, ModuleID module) {
    myModuleLoader = moduleLoader;
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
    final CompositeErrorReporter errorReporter = new CompositeErrorReporter();
    errorReporter.addErrorReporter(new LocalErrorReporter(new ModuleResolvedName(myModule), myErrorReporter));
    errorReporter.addErrorReporter(countingErrorReporter);

    VcgrammarLexer lexer = new VcgrammarLexer(new ANTLRInputStream(myStream));
    lexer.removeErrorListeners();
    lexer.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        errorReporter.report(new ParserError(new ModuleResolvedName(myModule), new Concrete.Position(line, pos), msg));
      }
    });

    VcgrammarParser parser = new VcgrammarParser(new CommonTokenStream(lexer));
    parser.removeErrorListeners();
    parser.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        errorReporter.report(new ParserError(new ModuleResolvedName(myModule), new Concrete.Position(line, pos), msg));
      }
    });

    VcgrammarParser.StatementsContext tree = parser.statements();
    if (tree == null || countingErrorReporter.getErrorsNumber() != 0) {
      return new ModuleLoader.Result(null, true, countingErrorReporter.getErrorsNumber());
    }

    ModuleResolver moduleResolver = new LoadingModuleResolver(myModuleLoader);
    List<Concrete.Statement> statements = new BuildVisitor(errorReporter).visitStatements(tree);
    Concrete.ClassDefinition classDefinition = new Concrete.ClassDefinition(new Concrete.Position(0, 0), myModule.getModulePath().getName(), statements);
    classDefinition.setModuleID(myModule);
    for (Concrete.Statement statement : statements) {
      if (statement instanceof Concrete.DefineStatement) {
        ((Concrete.DefineStatement) statement).setParentDefinition(classDefinition);
      }
    }

    DefinitionResolveStaticModVisitor rsmVisitor = new DefinitionResolveStaticModVisitor(new ConcreteStaticModListener());
    rsmVisitor.visitClass(classDefinition, true);

    DefinitionResolveNameVisitor visitor = new DefinitionResolveNameVisitor(errorReporter, null, DummyNameResolver.getInstance(), moduleResolver);
    visitor.setResolveListener(new ConcreteResolveListener());
    visitor.visitModule(classDefinition, myModule);
    return new ModuleLoader.Result(Root.getModule(myModule), true, countingErrorReporter.getErrorsNumber());
  }
}
