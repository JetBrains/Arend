package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.serialization.ModuleSerialization;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ModuleLoader {
  private final ClassDefinition myRoot = new ClassDefinition("\\root", null);
  private final List<Module> myLoadingModules = new ArrayList<>();
  private SourceSupplier mySourceSupplier;
  private OutputSupplier myOutputSupplier;
  private boolean myRecompile;
  private final List<Module> myLoadedModules = new ArrayList<>();
  private final List<OutputUnit> myOutputUnits = new ArrayList<>();
  private final List<ModuleError> myErrors = new ArrayList<>();
  private final List<TypeCheckingError> myTypeCheckingErrors = new ArrayList<>();

  private static ModuleLoader INSTANCE = new ModuleLoader();

  public static ModuleLoader getInstance() {
    return INSTANCE;
  }

  public void init(SourceSupplier sourceSupplier, OutputSupplier outputSupplier, boolean recompile) {
    mySourceSupplier = sourceSupplier;
    myOutputSupplier = outputSupplier;
    myRecompile = recompile;
  }

  public ClassDefinition rootModule() {
    return myRoot;
  }

  public boolean isModuleLoaded(Module module) {
    return myLoadedModules.contains(module);
  }

  public List<OutputUnit> getOutputUnits() {
    return myOutputUnits;
  }

  public List<ModuleError> getErrors() {
    return myErrors;
  }

  public List<TypeCheckingError> getTypeCheckingErrors() {
    return myTypeCheckingErrors;
  }

  public ClassDefinition loadModule(Module module, boolean tryLoad) {
    if (myLoadingModules.contains(module)) {
      myErrors.add(new ModuleError(module, "modules dependencies form a cycle"));
      return null;
    }
    ClassDefinition moduleDefinition = module.getParent().getClass(module.getName(), myErrors);
    if (moduleDefinition == null) {
      return null;
    }

    Source source = mySourceSupplier.getSource(module);
    Output output = myOutputSupplier.getOutput(module);
    boolean compile;
    if (source.isAvailable()) {
      compile = myRecompile || !output.canRead() || source.lastModified() > output.lastModified();
    } else {
      output = myOutputSupplier.locateOutput(module);
      if (!output.canRead()) {
        if (!tryLoad) {
          myErrors.add(new ModuleError(module, "cannot find module"));
        }
        return null;
      }
      compile = false;
    }

    boolean ok = true;
    myLoadingModules.add(module);
    try {
      if (compile) {
        if (source.load(moduleDefinition)) {
          moduleDefinition.hasErrors(false);
          if (output.canWrite()) {
            for (int i = 0; i < myOutputUnits.size(); ++i) {
              if (moduleDefinition.isDescendantOf(myOutputUnits.get(i).module)) {
                output = null;
                break;
              }
              if (myOutputUnits.get(i).module.isDescendantOf(moduleDefinition)) {
                myOutputUnits.remove(i--);
              }
            }
            if (output != null) {
              myOutputUnits.add(new OutputUnit(moduleDefinition, output));
            }
          }
        }
      } else {
        int errorsNumber = output.read(moduleDefinition);
        if (errorsNumber != 0) {
          myErrors.add(new ModuleError(module, "module contains " + errorsNumber + (errorsNumber == 1 ? " error" : " errors")));
          ok = false;
        } else {
          System.out.println("[Loaded] " + moduleDefinition.getFullName());
        }
        moduleDefinition.hasErrors(false);
      }
    } catch (ModuleSerialization.DeserializationException e) {
      myErrors.add(new ModuleError(module, e.toString()));
      ok = false;
    } catch (IOException e) {
      myErrors.add(new ModuleError(module, ModuleError.ioError(e)));
      ok = false;
    }
    myLoadingModules.remove(myLoadingModules.size() - 1);
    myLoadedModules.add(module);

    return ok ? moduleDefinition : null;
  }

  public static class OutputUnit {
    public ClassDefinition module;
    public Output output;

    public OutputUnit(ClassDefinition module, Output output) {
      this.module = module;
      this.output = output;
    }
  }
}
