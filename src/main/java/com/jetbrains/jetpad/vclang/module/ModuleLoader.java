package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.serialization.ModuleSerialization;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModuleLoader {
  private final ClassDefinition myRoot = new ClassDefinition("\\root", null);
  private final List<Module> myLoadingModules = new ArrayList<>();
  private SourceSupplier mySourceSupplier;
  private OutputSupplier myOutputSupplier;
  private boolean myRecompile;
  private final Set<Module> myLoadedModules = new HashSet<>();
  private final List<OutputUnit> myOutputUnits = new ArrayList<>();
  private final List<ModuleError> myErrors = new ArrayList<>();
  private final List<TypeCheckingError> myTypeCheckingErrors = new ArrayList<>();

  public void init(SourceSupplier sourceSupplier, OutputSupplier outputSupplier, boolean recompile) {
    Prelude.PRELUDE.setParent(myRoot);
    myRoot.addStaticField(Prelude.PRELUDE, null);
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
    int index = myLoadingModules.indexOf(module);
    if (index != -1) {
      String msg = "modules dependencies form a cycle: ";
      for (; index < myLoadingModules.size(); ++index) {
        msg += myLoadingModules.get(index) + " - ";
      }
      myErrors.add(new ModuleError(module, msg + module));
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

    ClassDefinition moduleDefinition = module.getParent().getClass(module.getName(), myErrors);
    if (moduleDefinition == null) {
      return null;
    }

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
        } else {
          System.out.println("[Loaded] " + moduleDefinition.getFullName());
        }
        moduleDefinition.hasErrors(false);
      }
    } catch (ModuleSerialization.DeserializationException e) {
      myErrors.add(new ModuleError(module, e.toString()));
    } catch (IOException e) {
      myErrors.add(new ModuleError(module, ModuleError.ioError(e)));
    }
    myLoadingModules.remove(myLoadingModules.size() - 1);
    myLoadedModules.add(module);

    if (moduleDefinition.hasErrors()) {
      module.getParent().removeField(moduleDefinition);
    } else {
      module.getParent().addStaticField(moduleDefinition, myErrors);
    }
    return moduleDefinition;
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
