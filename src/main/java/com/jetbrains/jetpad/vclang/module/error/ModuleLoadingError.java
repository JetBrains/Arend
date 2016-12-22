package com.jetbrains.jetpad.vclang.module.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.module.source.SourceId;

public class ModuleLoadingError extends GeneralError {
  public final SourceId module;

  public ModuleLoadingError(SourceId module, String message) {
    super(message, null);
    this.module = module;
  }
}
