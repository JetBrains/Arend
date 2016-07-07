package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.module.error.ModuleLoadingError;
import com.jetbrains.jetpad.vclang.module.utils.LoadModulesRecursively;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.error.GeneralError;

public interface ModuleLoader {
  Result load(ModuleID module);
  ModuleID locateModule(ModulePath modulePath);

  void save(ModuleID module);
  void savingError(GeneralError error);
  void loadingError(GeneralError error);
  void loadingSucceeded(ModuleID module, Abstract.ClassDefinition abstractDefinition, ClassDefinition compiledDefinition, boolean compiled);

  class Helper {
    static void processLoaded(ModuleLoader moduleLoader, ModuleID module, Result result) {
      if (result == null || result.errorsNumber != 0) {
        GeneralError error = new ModuleLoadingError(module, result == null ? "Cannot load module" : "Module  contains " + result.errorsNumber + (result.errorsNumber == 1 ? " error" : " errors"));
        moduleLoader.loadingError(error);
      } else {
        if (result.abstractDefinition != null) {
          new LoadModulesRecursively(moduleLoader).visitClass(result.abstractDefinition, null);
        }
        if (result.abstractDefinition != null || result.compiledDefinition != null) {
          moduleLoader.loadingSucceeded(module, result.abstractDefinition, result.compiledDefinition, result.compiled);
        }
      }
    }
  }

  class Result {
    public final Abstract.ClassDefinition abstractDefinition;
    public final ClassDefinition compiledDefinition;
    public final boolean compiled;
    public final int errorsNumber;

    public Result(Abstract.ClassDefinition abstractDefinition, ClassDefinition compiledDefinition, boolean compiled, int errorsNumber) {
      this.abstractDefinition = abstractDefinition;
      this.compiledDefinition = compiledDefinition;
      this.compiled = compiled;
      this.errorsNumber = errorsNumber;
    }
  }
}
