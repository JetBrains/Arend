package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.parser.BuildVisitor;
import com.jetbrains.jetpad.vclang.parser.ParserError;
import com.jetbrains.jetpad.vclang.parser.VcgrammarLexer;
import com.jetbrains.jetpad.vclang.parser.VcgrammarParser;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import org.antlr.v4.runtime.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public abstract class ParseSource implements Source {
  private final ModuleLoader myModuleLoader;
  private final Module myModule;
  private InputStream myStream;

  public ParseSource(ModuleLoader moduleLoader, Module module) {
    myModuleLoader = moduleLoader;
    myModule = module;
  }

  public InputStream getStream() {
    return myStream;
  }

  public void setStream(InputStream stream) {
    myStream = stream;
  }

  @Override
  public boolean load(ClassDefinition classDefinition) throws IOException {
    VcgrammarParser parser = new VcgrammarParser(new CommonTokenStream(new VcgrammarLexer(new ANTLRInputStream(myStream))));
    parser.removeErrorListeners();
    int errorsCount = myModuleLoader.getErrors().size();
    parser.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        myModuleLoader.getErrors().add(new ParserError(myModule, new Concrete.Position(line, pos), msg));
      }
    });

    VcgrammarParser.DefsContext tree = parser.defs();
    if (errorsCount != myModuleLoader.getErrors().size()) return false;
    new BuildVisitor(classDefinition, myModuleLoader).visitDefs(tree);
    return errorsCount == myModuleLoader.getErrors().size();
  }
}
