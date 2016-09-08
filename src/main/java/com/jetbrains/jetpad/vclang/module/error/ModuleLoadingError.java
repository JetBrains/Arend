package com.jetbrains.jetpad.vclang.module.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.module.source.ModuleSourceId;

public class ModuleLoadingError extends GeneralError {
  public final ModuleSourceId module;

  public ModuleLoadingError(ModuleSourceId module, String message) {
    super(message, null);
    this.module = module;
  }
}
