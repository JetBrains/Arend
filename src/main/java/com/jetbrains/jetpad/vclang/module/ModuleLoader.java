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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleLoader {
  private final ClassDefinition myRoot = new ClassDefinition("\\root", null, new HashMap<String, Definition>());
  private final List<Module> myLoadingModules = new ArrayList<>();
  private File mySourceDir;
  private File myOutputDir;
  private List<File> myLibDirs;
  private boolean myRecompile;
  private final List<Module> myLoadedModules = new ArrayList<>();
  private final List<TypeCheckingUnit> myTypeCheckingUnits = new ArrayList<>();
  private final List<OutputUnit> myOutputUnits = new ArrayList<>();
  private final List<ModuleError> myErrors = new ArrayList<>();

  private static ModuleLoader INSTANCE = new ModuleLoader();

  public static ModuleLoader getInstance() {
    return INSTANCE;
  }

  public void init(File sourceDir, File outputDir, List<File> libDirs, boolean recompile) {
    myRoot.add(Prelude.PRELUDE);
    mySourceDir = sourceDir;
    myOutputDir = outputDir;
    myLibDirs = libDirs;
    myRecompile = recompile;
  }

  public ClassDefinition rootModule() {
    return myRoot;
  }

  public boolean isModuleLoaded(Module module) {
    return myLoadedModules.contains(module);
  }

  public List<TypeCheckingUnit> getTypeCheckingUnits() {
    return myTypeCheckingUnits;
  }

  public List<OutputUnit> getOutputUnits() {
    return myOutputUnits;
  }

  public List<ModuleError> getErrors() {
    return myErrors;
  }

  public Concrete.ClassDefinition loadSource(final Module module, ClassDefinition moduleDef, File sourceFile, final List<ModuleError> myErrors) throws IOException {
    VcgrammarParser parser = new VcgrammarParser(new CommonTokenStream(new VcgrammarLexer(new ANTLRInputStream(new FileInputStream(sourceFile)))));
    parser.removeErrorListeners();
    int errorsCount = myErrors.size();
    parser.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        myErrors.add(new ParserError(module, new Concrete.Position(line, pos), msg));
      }
    });

    VcgrammarParser.DefsContext tree = parser.defs();
    if (errorsCount != myErrors.size()) return null;
    Map<String, Concrete.Definition> defs = new BuildVisitor(module, moduleDef, INSTANCE).visitDefs(tree);
    if (errorsCount != myErrors.size()) return null;
    return new Concrete.ClassDefinition(new Concrete.Position(0, 0), module.getName(), null, defs);
  }

  public ClassDefinition getModule(ClassDefinition parent, String name) {
    Definition definition = parent.findChild(name);
    if (definition == null) {
      ClassDefinition result = new ClassDefinition(name, parent, new HashMap<String, Definition>());
      parent.add(result);
      result.hasErrors(true);
      return result;
    } else {
      if (definition instanceof ClassDefinition) {
        return (ClassDefinition) definition;
      } else {
        myErrors.add(new ModuleError(new Module(parent, name), name + " is already defined"));
        return null;
      }
    }
  }

  public ClassDefinition loadModule(Module module) {
    return loadModule(module, false);
  }

  public ClassDefinition loadModule(Module module, boolean tryLoad) {
    if (myLoadingModules.contains(module)) {
      myErrors.add(new ModuleError(module, "modules dependencies form a cycle"));
      return null;
    }
    ClassDefinition moduleDefinition = getModule(module.getParent(), module.getName());
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
          myErrors.add(new ModuleError(module, "cannot find module"));
        }
        if (moduleDefinition.hasErrors()) {
          module.getParent().getFields().remove(moduleDefinition.getName());
        }
        return null;
      }
      compile = false;
    }

    myLoadingModules.add(module);
    try {
      if (compile) {
        Concrete.ClassDefinition rawClass = loadSource(module, moduleDefinition, sourceFile, myErrors);
        if (rawClass != null) {
          myTypeCheckingUnits.add(new TypeCheckingUnit(rawClass, moduleDefinition));
          moduleDefinition.hasErrors(false);
          if (outputFile != null) {
            for (int i = 0; i < myOutputUnits.size(); ++i) {
              if (moduleDefinition.isDescendantOf(myOutputUnits.get(i).module)) {
                outputFile = null;
                break;
              }
              if (myOutputUnits.get(i).module.isDescendantOf(moduleDefinition)) {
                myOutputUnits.remove(i--);
              }
            }
            if (outputFile != null) {
              myOutputUnits.add(new OutputUnit(moduleDefinition, outputFile));
            }
          }
        }
      } else {
        int errorsNumber = ModuleSerialization.readFile(outputFile, moduleDefinition);
        if (errorsNumber != 0) {
          myErrors.add(new ModuleError(module, "module contains " + errorsNumber + (errorsNumber == 1 ? " error" : " errors")));
        }
        moduleDefinition.hasErrors(false);
      }
    } catch (IOException e) {
      myErrors.add(new ModuleError(module, ModuleError.ioError(e)));
    } catch (ModuleSerialization.DeserializationException e) {
      myErrors.add(new ModuleError(module, e.toString()));
    }
    myLoadingModules.remove(myLoadingModules.size() - 1);
    myLoadedModules.add(module);

    if (moduleDefinition.hasErrors()) {
      module.getParent().getFields().remove(moduleDefinition.getName());
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
