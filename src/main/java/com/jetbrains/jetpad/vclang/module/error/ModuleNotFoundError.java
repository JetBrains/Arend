package com.jetbrains.jetpad.vclang.module.error;

import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.naming.ModuleResolvedName;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;

public class ModuleNotFoundError extends GeneralError {
  public ModuleNotFoundError(ModuleID module) {
    super(new ModuleResolvedName(module), "cannot find module '" + module + "'");
  }
}