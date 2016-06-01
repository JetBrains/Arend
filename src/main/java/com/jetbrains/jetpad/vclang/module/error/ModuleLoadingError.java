package com.jetbrains.jetpad.vclang.module.error;

import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.error.GeneralError;

public class ModuleLoadingError extends GeneralError {
  public final ModuleID module;

  public ModuleLoadingError(ModuleID module, String message) {
    super(message, null);
    this.module = module;
  }
}
