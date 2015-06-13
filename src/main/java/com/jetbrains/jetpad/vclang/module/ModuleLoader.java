package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.VcError;
import com.jetbrains.jetpad.vclang.parser.BuildVisitor;
import com.jetbrains.jetpad.vclang.parser.ParserError;
import com.jetbrains.jetpad.vclang.parser.VcgrammarLexer;
import com.jetbrains.jetpad.vclang.parser.VcgrammarParser;
import com.jetbrains.jetpad.vclang.serialization.ModuleSerialization;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionCheckTypeVisitor;
import org.antlr.v4.runtime.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ModuleLoader {
  private final static ClassDefinition myRoot = new ClassDefinition("\\root", null, new Universe.Type(0), new ArrayList<Definition>());
  private final static List<Module> myLoadingModules = new ArrayList<>();
  private static File mySourceDir;
  private static File myOutputDir;
  private static List<File> myLibDirs;
  private static boolean myRecompile;

  private ModuleLoader() {}

  public static void init(File sourceDir, File outputDir, List<File> libDirs, boolean recompile) {
    mySourceDir = sourceDir;
    myOutputDir = outputDir;
    myLibDirs = libDirs;
    myRecompile = recompile;
  }

  public static ClassDefinition rootModule() {
    return myRoot;
  }

  private static ClassDefinition loadSource(ClassDefinition parent, String moduleName, File sourceFile, File outputFile, final List<VcError> errors) throws IOException {
    VcgrammarParser parser = new VcgrammarParser(new CommonTokenStream(new VcgrammarLexer(new ANTLRInputStream(new FileInputStream(sourceFile)))));
    parser.removeErrorListeners();
    int errorsCount = errors.size();
    parser.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        errors.add(new ParserError(new Concrete.Position(line, pos), msg));
      }
    });
    VcgrammarParser.DefsContext tree = parser.defs();
    if (errorsCount != errors.size()) return null;
    List<Concrete.Definition> defs = new BuildVisitor(errors).visitDefs(tree);
    if (errorsCount != errors.size()) return null;

    Map<String, Definition> context = Prelude.getDefinitions();
    Concrete.ClassDefinition classDef = new Concrete.ClassDefinition(new Concrete.Position(0, 0), moduleName, new Universe.Type(), defs);
    ClassDefinition result = new DefinitionCheckTypeVisitor(parent, context, errors).visitClass(classDef, new ArrayList<Binding>());

    if (outputFile != null) {
      ModuleSerialization.writeFile(result, outputFile);
    }

    return result;
  }

  private static ClassDefinition loadCompiled(File file) {
    return null;
  }

  public static ClassDefinition loadModule(Module module, List<VcError> errors) {
    if (myLoadingModules.contains(module)) {
      errors.add(new ModuleError(module, "modules dependencies form a cycle"));
      return null;
    }
    myLoadingModules.add(module);
    ClassDefinition parent = module.getParent();

    File sourceFile = mySourceDir == null ? null : module.getFile(mySourceDir, ".vc");
    File outputFile = myOutputDir == null ? null : module.getFile(myOutputDir, ".vcc");
    boolean compile;
    if (sourceFile != null && sourceFile.exists()) {
      compile = myRecompile || outputFile == null || !outputFile.exists() || sourceFile.lastModified() > outputFile.lastModified();
    } else {
      for (File libDir : myLibDirs) {
        if (outputFile != null && outputFile.exists()) {
          break;
        }
        outputFile = module.getFile(libDir, ".vcc");
      }
      if (outputFile == null || !outputFile.exists()) {
        errors.add(new ModuleError(module, "cannot find module"));
        return null;
      }
      compile = false;
    }

    ClassDefinition result = null;
    try {
      result = compile ? loadSource(parent, module.getName(), sourceFile, outputFile, errors) : loadCompiled(outputFile);
    } catch (IOException e) {
      errors.add(new VcError(VcError.ioError(e)));
    }
    myLoadingModules.remove(myLoadingModules.size() - 1);
    return result;
  }
}
