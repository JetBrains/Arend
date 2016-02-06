package com.jetbrains.jetpad.vclang.module.output;

import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.module.ModulePath;

public interface OutputSupplier {
  Output getOutput(ModuleID module);
  ModuleID locateModule(ModulePath modulePath);
}
