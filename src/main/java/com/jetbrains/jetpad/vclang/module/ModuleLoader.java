package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.parser.BuildVisitor;
import com.jetbrains.jetpad.vclang.parser.ParserError;
import com.jetbrains.jetpad.vclang.parser.VcgrammarLexer;
import com.jetbrains.jetpad.vclang.parser.VcgrammarParser;
import com.jetbrains.jetpad.vclang.serialization.ModuleSerialization;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
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
    ModuleLoader.rootModule().getFields().add(Prelude.PRELUDE);
    mySourceDir = sourceDir;
    myOutputDir = outputDir;
    myLibDirs = libDirs;
    myRecompile = recompile;
  }

  public static ClassDefinition rootModule() {
    return myRoot;
  }

  public static File sourceDir() {
    return mySourceDir;
  }

  public static Concrete.ClassDefinition loadSource(final Module module, ClassDefinition moduleDef, File sourceFile, List<TypeCheckingUnit> typeCheckingUnits, List<OutputUnit> outputUnits, final List<ModuleError> errors) throws IOException {
    VcgrammarParser parser = new VcgrammarParser(new CommonTokenStream(new VcgrammarLexer(new ANTLRInputStream(new FileInputStream(sourceFile)))));
    parser.removeErrorListeners();
    int errorsCount = errors.size();
    parser.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        errors.add(new ParserError(module, new Concrete.Position(line, pos), msg));
      }
    });

    VcgrammarParser.DefsContext tree = parser.defs();
    if (errorsCount != errors.size()) return null;
    List<Concrete.Definition> defs = new BuildVisitor(module, moduleDef, typeCheckingUnits, outputUnits, errors).visitDefs(tree);
    if (errorsCount != errors.size()) return null;
    return new Concrete.ClassDefinition(new Concrete.Position(0, 0), module.getName(), null, defs);
  }

  public static ClassDefinition getModule(ClassDefinition parent, String name, List<ModuleError> errors) {
    Definition definition = parent.findChild(name);
    if (definition == null) {
      ClassDefinition result = new ClassDefinition(name, parent, new ArrayList<Definition>());
      parent.getFields().add(result);
      result.hasErrors(true);
      return result;
    } else {
      if (definition instanceof ClassDefinition) {
        return (ClassDefinition) definition;
      } else {
        errors.add(new ModuleError(new Module(parent, name), name + " is already defined"));
        return null;
      }
    }
  }

  public static ClassDefinition loadModule(Module module, List<TypeCheckingUnit> typeCheckingUnits, List<OutputUnit> outputUnits, List<ModuleError> errors) {
    return loadModule(module, typeCheckingUnits, outputUnits, errors, false);
  }

  public static ClassDefinition loadModule(Module module, List<TypeCheckingUnit> typeCheckingUnits, List<OutputUnit> outputUnits, List<ModuleError> errors, boolean tryLoad) {
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
        if (!tryLoad) {
          errors.add(new ModuleError(module, "cannot find module"));
        }
        if (moduleDefinition.hasErrors()) {
          module.getParent().getFields().remove(moduleDefinition);
        }
        return null;
      }
      compile = false;
    }

    myLoadingModules.add(module);
    try {
      if (compile) {
        Concrete.ClassDefinition rawClass = loadSource(module, moduleDefinition, sourceFile, typeCheckingUnits, outputUnits, errors);
        if (rawClass != null) {
          typeCheckingUnits.add(new TypeCheckingUnit(rawClass, moduleDefinition));
          moduleDefinition.hasErrors(false);
          if (outputFile != null) {
            for (int i = 0; i < outputUnits.size(); ++i) {
              if (moduleDefinition.isDescendantOf(outputUnits.get(i).module)) {
                outputFile = null;
                break;
              }
              if (outputUnits.get(i).module.isDescendantOf(moduleDefinition)) {
                outputUnits.remove(i--);
              }
            }
            if (outputFile != null) {
              outputUnits.add(new OutputUnit(moduleDefinition, outputFile));
            }
          }
        }
      } else {
        int errorsNumber = ModuleSerialization.readFile(outputFile, moduleDefinition, typeCheckingUnits, outputUnits, errors);
        if (errorsNumber != 0) {
          errors.add(new ModuleError(module, "module contains " + errorsNumber + (errorsNumber == 1 ? " error" : " errors")));
        }
        moduleDefinition.hasErrors(false);
      }
    } catch (IOException e) {
      errors.add(new ModuleError(module, ModuleError.ioError(e)));
    } catch (ModuleSerialization.DeserializationException e) {
      errors.add(new ModuleError(module, e.toString()));
    }
    myLoadingModules.remove(myLoadingModules.size() - 1);

    if (moduleDefinition.hasErrors()) {
      module.getParent().getFields().remove(moduleDefinition);
      return null;
    } else {
      return moduleDefinition;
    }
  }

  public static class OutputUnit {
    public ClassDefinition module;
    public File file;

    public OutputUnit(ClassDefinition module, File file) {
      this.module = module;
      this.file = file;
    }
  }

  public static class TypeCheckingUnit {
    public Concrete.Definition rawDefinition;
    public Definition typedDefinition;

    public TypeCheckingUnit(Concrete.Definition rawDefinition, Definition typedDefinition) {
      this.rawDefinition = rawDefinition;
      this.typedDefinition = typedDefinition;
    }
  }
}
