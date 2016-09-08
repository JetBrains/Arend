package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.module.error.ModuleLoadingError;
import com.jetbrains.jetpad.vclang.module.utils.LoadModulesRecursively;
import com.jetbrains.jetpad.vclang.term.Abstract;

public interface ModuleLoader {
  Result load(ModuleID module);
  ModuleID locateModule(ModulePath modulePath);

  void loadingError(GeneralError error);
  void loadingSucceeded(ModuleID module, Abstract.ClassDefinition abstractDefinition);

  class Helper {
    static void processLoaded(ModuleLoader moduleLoader, ModuleID module, Result result) {
      if (result == null || result.errorsNumber != 0) {
        GeneralError error = new ModuleLoadingError(module, result == null ? "Cannot load module" : "Module  contains " + result.errorsNumber + (result.errorsNumber == 1 ? " error" : " errors"));
        moduleLoader.loadingError(error);
      } else {
        if (result.abstractDefinition != null) {
          new LoadModulesRecursively(moduleLoader).visitClass(result.abstractDefinition, null);
          moduleLoader.loadingSucceeded(module, result.abstractDefinition);
        }
      }
    }
  }

  class Result {
    public final Abstract.ClassDefinition abstractDefinition;
    public final int errorsNumber;

    public Result(Abstract.ClassDefinition abstractDefinition, int errorsNumber) {
      this.abstractDefinition = abstractDefinition;
      this.errorsNumber = errorsNumber;
    }
  }
}
