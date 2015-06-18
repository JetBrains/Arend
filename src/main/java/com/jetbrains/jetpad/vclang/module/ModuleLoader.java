package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.VcError;
import com.jetbrains.jetpad.vclang.parser.BuildVisitor;
import com.jetbrains.jetpad.vclang.parser.ParserError;
import com.jetbrains.jetpad.vclang.parser.VcgrammarLexer;
import com.jetbrains.jetpad.vclang.parser.VcgrammarParser;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionCheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import org.antlr.v4.runtime.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ModuleLoader {
  private final static ClassDefinition myRoot = new ClassDefinition("\\root", null, new ArrayList<Definition>());
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

  public static Concrete.ClassDefinition loadSource(Module module, ClassDefinition moduleDef, File sourceFile, List<TypeCheckingUnit> units, final List<VcError> errors) throws IOException {
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
    List<Concrete.Definition> defs = new BuildVisitor(moduleDef, units, errors).visitDefs(tree);
    if (errorsCount != errors.size()) return null;
    return new Concrete.ClassDefinition(new Concrete.Position(0, 0), module.getName(), null, defs);
  }

  public static void loadCompiled(File file, ClassDefinition module) {
  }

  public static ClassDefinition getModule(ClassDefinition parent, String name, List<VcError> errors) {
    Definition definition = parent.findField(name);
    if (definition == null) {
      ClassDefinition result = new ClassDefinition(name, parent, new ArrayList<Definition>());
      parent.getFields().add(result);
      return result;
    } else {
      if (definition instanceof ClassDefinition) {
        return  (ClassDefinition) definition;
      } else {
        errors.add(new VcError(name + " is already defined"));
        return null;
      }
    }
  }

  public static ClassDefinition loadModule(Module module, List<TypeCheckingUnit> units, List<VcError> errors) {
    if (myLoadingModules.contains(module)) {
      errors.add(new ModuleError(module, "modules dependencies form a cycle"));
      return null;
    }
    ClassDefinition moduleDefinition = getModule(module.getParent(), module.getName(), errors);
    if (moduleDefinition == null) {
      return null;
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
        errors.add(new ModuleError(module, "cannot find module"));
        return null;
      }
      compile = false;
    }

    myLoadingModules.add(module);
    try {
      if (compile) {
        Concrete.ClassDefinition rawClass = loadSource(module, moduleDefinition, sourceFile, units, errors);
        if (rawClass != null) {
          units.add(new TypeCheckingUnit(rawClass, moduleDefinition));
        }
      } else {
        loadCompiled(outputFile, moduleDefinition);
      }
    } catch (IOException e) {
      errors.add(new VcError(VcError.ioError(e)));
    }
    myLoadingModules.remove(myLoadingModules.size() - 1);
    return moduleDefinition;
  }

  public static class TypeCheckingUnit {
    public Concrete.Definition rawDefinition;
    public Definition typedDefinition;

    public TypeCheckingUnit(Concrete.Definition rawDefinition, Definition typedDefinition) {
      this.rawDefinition = rawDefinition;
      this.typedDefinition = typedDefinition;
    }
  }

  public static void typeCheck(List<TypeCheckingUnit> units, List<TypeCheckingError> errors) {
    for (TypeCheckingUnit unit : units) {
      unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(unit.typedDefinition, errors), new ArrayList<Binding>());
      // ModuleSerialization.writeFile((ClassDefinition) unit.typedDefinition, unit.outputFile);
    }
  }
}
