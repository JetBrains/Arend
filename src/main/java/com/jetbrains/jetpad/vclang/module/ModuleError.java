package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.VcError;

public class ModuleError extends VcError {
  private final Module myModule;

  public ModuleError(Module module, String message) {
    super(message);
    myModule = module;
  }

  @Override
  public String toString() {
    return myModule + ": " + (getMessage() == null ? "Unknown error" : getMessage());
  }
}
