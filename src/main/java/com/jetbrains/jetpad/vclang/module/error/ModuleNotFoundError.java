package com.jetbrains.jetpad.vclang.module.error;

import com.jetbrains.jetpad.vclang.module.ModuleID;

public class ModuleNotFoundError extends ModuleLoadingError {
  public ModuleNotFoundError(ModuleID module) {
    super(module, "Module not found");
  }
}