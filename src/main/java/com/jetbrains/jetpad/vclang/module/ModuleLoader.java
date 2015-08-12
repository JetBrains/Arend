package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.serialization.ModuleDeserialization;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Namespace;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModuleLoader {
  private final Namespace myRoot = new Namespace(new Utils.Name("\\root"), null);
  private final List<Module> myLoadingModules = new ArrayList<>();
  private SourceSupplier mySourceSupplier;
  private OutputSupplier myOutputSupplier;
  private boolean myRecompile;
  private final Set<Module> myLoadedModules = new HashSet<>();
  private final List<ModuleError> myErrors = new ArrayList<>();
  private final List<TypeCheckingError> myTypeCheckingErrors = new ArrayList<>();

  public void init(SourceSupplier sourceSupplier, OutputSupplier outputSupplier, boolean recompile) {
    Prelude.PRELUDE.setParent(myRoot);
    myRoot.addChild(Prelude.PRELUDE);
    mySourceSupplier = sourceSupplier;
    myOutputSupplier = outputSupplier;
    myRecompile = recompile;
  }

  public Namespace getRoot() {
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

    myLoadingModules.add(module);
    Namespace namespace = module.getParent().getChild(new Utils.Name(module.getName()));
    ClassDefinition classDefinition = null;
    if (module.getParent().getMember(module.getName()) == null) {
      classDefinition = new ClassDefinition(namespace);
    }
    try {
      if (compile) {
        int errors = myTypeCheckingErrors.size();
        if (source.load(namespace, classDefinition)) {
          if (output.canWrite()) {
            output.write(namespace, classDefinition);
          }
          if (errors == myTypeCheckingErrors.size()) {
            System.out.println("[OK] " + namespace.getFullName());
          }
        }
      } else {
        int errorsNumber = output.read(namespace, classDefinition);
        if (errorsNumber != 0) {
          myErrors.add(new ModuleError(module, "module contains " + errorsNumber + (errorsNumber == 1 ? " error" : " errors")));
        } else {
          System.out.println("[Loaded] " + namespace.getFullName());
        }
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

    if (classDefinition != null) {
      module.getParent().addMember(classDefinition);
    }
    return classDefinition;
  }
}
