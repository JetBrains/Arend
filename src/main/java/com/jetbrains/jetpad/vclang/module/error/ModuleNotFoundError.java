package com.jetbrains.jetpad.vclang.module.error;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;

public class ModuleNotFoundError extends GeneralError {
  public ModuleNotFoundError(Namespace namespace) {
    super(namespace, "cannot find module");
  }
}
