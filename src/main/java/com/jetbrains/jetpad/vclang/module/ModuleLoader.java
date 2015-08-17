package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.serialization.ModuleDeserialization;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;

import java.io.EOFException;
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
  private List<ModuleError> myErrors;
  private List<TypeCheckingError> myTypeCheckingErrors;

  public void init(SourceSupplier sourceSupplier, OutputSupplier outputSupplier, boolean recompile) {
    init(sourceSupplier, outputSupplier, new ArrayList<ModuleError>(), new ArrayList<TypeCheckingError>(), recompile);
  }

  public void init(SourceSupplier sourceSupplier, OutputSupplier outputSupplier,
                   List<ModuleError> errors, List<TypeCheckingError> typeCheckingErrors, boolean recompile) {
    Prelude.PRELUDE.setParent(myRoot);
    myRoot.addStaticField(Prelude.PRELUDE, null);
    mySourceSupplier = sourceSupplier;
    myOutputSupplier = outputSupplier;
    myErrors = errors;
    myTypeCheckingErrors = typeCheckingErrors;
    myRecompile = recompile;
  }

  public ClassDefinition rootModule() {
    return myRoot;
  }

  public List<ModuleError> getErrors() {
    return myErrors;
  }

  public List<TypeCheckingError> getTypeCheckingErrors() {
    return myTypeCheckingErrors;
  }

  public ClassDefinition loadModule(Module module, boolean tryLoad) {
    if (myLoadedModules.contains(module)) {
      return null;
    }

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

    module.getParent().addStaticField(moduleDefinition, myErrors);
    myLoadingModules.add(module);
    try {
      if (compile) {
        int errors = myTypeCheckingErrors.size();
        if (source.load(moduleDefinition)) {
          moduleDefinition.hasErrors(false);
          if (output.canWrite()) {
            output.write(moduleDefinition);
          }
          if (errors == myTypeCheckingErrors.size()) {
            System.out.println("[OK] " + moduleDefinition.getFullName());
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
    } catch (EOFException e) {
      myErrors.add(new ModuleError(module, "Incorrect format: Unexpected EOF"));
    } catch (ModuleDeserialization.DeserializationException e) {
      myErrors.add(new ModuleError(module, e.toString()));
    } catch (IOException e) {
      myErrors.add(new ModuleError(module, ModuleError.ioError(e)));
    }
    myLoadingModules.remove(myLoadingModules.size() - 1);
    myLoadedModules.add(module);

    if (moduleDefinition.hasErrors()) {
      module.getParent().removeField(moduleDefinition);
    }
    return moduleDefinition;
  }
}
