package com.jetbrains.jetpad.vclang.module.error;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.source.ModuleSourceId;

import java.util.List;

public class ModuleCycleError extends ModuleLoadingError {
  public final List<ModulePath> cycle;

  public ModuleCycleError(ModuleSourceId module, List<ModulePath> cycle) {
    super(module, "Module dependencies form a cycle");
    this.cycle = cycle;
  }
}
