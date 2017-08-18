package com.jetbrains.jetpad.vclang.module.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.module.source.SourceId;

public class ModuleLoadingError<T> extends GeneralError<T> {
  public final SourceId module;

  public ModuleLoadingError(SourceId module, String message) {
    super(Level.ERROR, message);
    this.module = module;
  }
}
