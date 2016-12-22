package com.jetbrains.jetpad.vclang.module.error;

import com.jetbrains.jetpad.vclang.module.source.SourceId;

public class ModuleNotFoundError extends ModuleLoadingError {
  public ModuleNotFoundError(SourceId module) {
    super(module, "Module not found");
  }
}