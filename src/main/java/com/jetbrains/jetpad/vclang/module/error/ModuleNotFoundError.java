package com.jetbrains.jetpad.vclang.module.error;

import com.jetbrains.jetpad.vclang.module.source.ModuleSourceId;

public class ModuleNotFoundError extends ModuleLoadingError {
  public ModuleNotFoundError(ModuleSourceId module) {
    super(module, "Module not found");
  }
}