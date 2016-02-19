package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.module.ModulePath;

public interface SourceSupplier {
  Source getSource(ModuleID module);
  ModuleID locateModule(ModulePath modulePath);
}
