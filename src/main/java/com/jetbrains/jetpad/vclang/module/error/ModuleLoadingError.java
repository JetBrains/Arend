package com.jetbrains.jetpad.vclang.module.error;

import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.error.GeneralError;

public class ModuleLoadingError extends GeneralError {
  private final ModuleID myModule;

  public ModuleLoadingError(ModuleID module, String message) {
    super(message);
    myModule = module;
  }

  @Override
  public String printHeader() {
    return super.printHeader() + "Loading '" + (myModule != null ? myModule.getModulePath() : "<Unknown module>") + "': ";
  }
}
