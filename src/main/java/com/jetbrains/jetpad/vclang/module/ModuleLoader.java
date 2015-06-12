package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.parser.BuildVisitor;
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
import com.jetbrains.jetpad.vclang.term.error.ParserError;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import org.antlr.v4.runtime.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ModuleLoader {
  private final ClassDefinition myRoot = new ClassDefinition("\\root", null, new Universe.Type(0), new ArrayList<Definition>());
  private final List<Module> myLoadingModules = new ArrayList<>();
  private final File mySourceDir;
  private final File myOutputDir;
  private final List<File> myLibDirs;
  private final boolean myRecompile;

  public ModuleLoader(File sourceDir, File outputDir, List<File> libDirs, boolean recompile) {
    mySourceDir = sourceDir;
    myOutputDir = outputDir;
    myLibDirs = libDirs;
    myRecompile = recompile;
  }

  private ClassDefinition loadSource(ClassDefinition parent, String moduleName, File sourceFile, File outputFile, final List<ParserError> parserErrors, List<TypeCheckingError> errors) throws IOException {
    VcgrammarParser parser = new VcgrammarParser(new CommonTokenStream(new VcgrammarLexer(new ANTLRInputStream(new FileInputStream(sourceFile)))));
    parser.removeErrorListeners();
    parser.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        parserErrors.add(new ParserError(new Concrete.Position(line, pos), msg));
      }
    });
    VcgrammarParser.DefsContext tree = parser.defs();
    List<Concrete.Definition> defs = parserErrors.isEmpty() ? new BuildVisitor().visitDefs(tree) : null;
    if (!parserErrors.isEmpty()) return null;

    Map<String, Definition> context = Prelude.getDefinitions();
    Concrete.ClassDefinition classDef = new Concrete.ClassDefinition(new Concrete.Position(0, 0), moduleName, new Universe.Type(), defs);
    ClassDefinition result = new DefinitionCheckTypeVisitor(parent, context, errors).visitClass(classDef, new ArrayList<Binding>());

    if (outputFile != null) {
      ModuleSerialization.writeFile(result, outputFile);
    }

    return result;
  }

  private ClassDefinition loadCompiled(File file) {
    return null;
  }

  public ClassDefinition loadModule(Module module, List<ModuleError> moduleErrors, List<ParserError> parserErrors, List<TypeCheckingError> errors) throws IOException {
    if (myLoadingModules.contains(module)) {
      moduleErrors.add(new ModuleError(module, "modules dependencies form a cycle"));
      return null;
    }
    myLoadingModules.add(module);
    Module parentModule = module.getParent();
    ClassDefinition parent = myRoot;
    if (parentModule != null) {
      parent = loadModule(parentModule, moduleErrors, parserErrors, errors);
      if (parent == null) return null;
    }

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
        moduleErrors.add(new ModuleError(module, "cannot find module"));
        return null;
      }
      compile = false;
    }

    ClassDefinition result = compile ? loadSource(parent, module.getName(), sourceFile, outputFile, parserErrors, errors) : loadCompiled(outputFile);
    myLoadingModules.remove(myLoadingModules.size() - 1);
    return result;
  }
}
