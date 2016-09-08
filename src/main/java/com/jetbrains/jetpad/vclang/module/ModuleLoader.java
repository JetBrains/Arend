package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.term.Abstract;

public interface ModuleLoader {
  ModuleID locateModule(ModulePath modulePath);

  Result load(ModuleID module);
  void loadingError(GeneralError error);
  void loadingSucceeded(ModuleID module, Abstract.ClassDefinition abstractDefinition);

  class Result {
    public final Abstract.ClassDefinition abstractDefinition;
    public final int errorsNumber;

    public Result(Abstract.ClassDefinition abstractDefinition, int errorsNumber) {
      this.abstractDefinition = abstractDefinition;
      this.errorsNumber = errorsNumber;
    }
  }
}
