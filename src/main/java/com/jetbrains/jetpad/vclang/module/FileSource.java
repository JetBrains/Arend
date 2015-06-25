package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.parser.BuildVisitor;
import com.jetbrains.jetpad.vclang.parser.ParserError;
import com.jetbrains.jetpad.vclang.parser.VcgrammarLexer;
import com.jetbrains.jetpad.vclang.parser.VcgrammarParser;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import org.antlr.v4.runtime.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class FileSource implements Source {
  private final Module myModule;
  private final File myFile;

  public FileSource(Module module, File file) {
    myModule = module;
    myFile = file;
  }

  @Override
  public boolean isAvailable() {
    return myFile != null && myFile.exists();
  }

  @Override
  public long lastModified() {
    return myFile.lastModified();
  }

  @Override
  public Concrete.ClassDefinition load(ClassDefinition classDefinition) throws IOException {
    VcgrammarParser parser = new VcgrammarParser(new CommonTokenStream(new VcgrammarLexer(new ANTLRInputStream(new FileInputStream(myFile)))));
    parser.removeErrorListeners();
    int errorsCount = ModuleLoader.getInstance().getErrors().size();
    parser.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        ModuleLoader.getInstance().getErrors().add(new ParserError(myModule, new Concrete.Position(line, pos), msg));
      }
    });

    VcgrammarParser.DefsContext tree = parser.defs();
    if (errorsCount != ModuleLoader.getInstance().getErrors().size()) return null;
    List<Concrete.Definition> defs = new BuildVisitor(myModule, classDefinition, ModuleLoader.getInstance()).visitDefs(tree);
    if (errorsCount != ModuleLoader.getInstance().getErrors().size()) return null;
    return new Concrete.ClassDefinition(new Concrete.Position(0, 0), myModule.getName(), null, defs);
  }
}
