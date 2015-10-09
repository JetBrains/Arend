package com.jetbrains.jetpad.vclang.module.error;

import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;

public class ModuleNotFoundError extends GeneralError {
  public ModuleNotFoundError(ResolvedName resolvedName) {
    super(resolvedName, "cannot find module '" + resolvedName.name + "'");
  }
}
