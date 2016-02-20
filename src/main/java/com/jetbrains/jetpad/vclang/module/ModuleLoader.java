package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.naming.ModuleResolvedName;
import com.jetbrains.jetpad.vclang.naming.NamespaceMember;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;

public interface ModuleLoader {
  Result load(ModuleID module);
  ModuleID locateModule(ModulePath modulePath);

  void save(ModuleID module);
  void savingError(GeneralError error);
  void loadingError(GeneralError error);
  void loadingSucceeded(ModuleID module, NamespaceMember definition, boolean compiled);

  class Helper {
    static void processLoaded(ModuleLoader moduleLoader, ModuleID module, Result result) {
      if (result == null || result.errorsNumber != 0) {
        GeneralError error = new GeneralError(new ModuleResolvedName(module), result == null ? "cannot load module '" + module.getModulePath().getName() + "'" : "module '" + module.getModulePath().getName() + "' contains " + result.errorsNumber + (result.errorsNumber == 1 ? " error" : " errors"));
        error.setLevel(GeneralError.Level.INFO);
        moduleLoader.loadingError(error);
      } else {
        if (result.namespaceMember != null) {
          if (result.namespaceMember.abstractDefinition != null || result.namespaceMember.definition != null)
            moduleLoader.loadingSucceeded(module, result.namespaceMember, result.compiled);
        }
      }
    }
  }

  class Result {
    public NamespaceMember namespaceMember;
    public boolean compiled;
    public int errorsNumber;

    public Result(NamespaceMember namespaceMember, boolean compiled, int errorsNumber) {
      this.namespaceMember = namespaceMember;
      this.compiled = compiled;
      this.errorsNumber = errorsNumber;
    }
  }
}
