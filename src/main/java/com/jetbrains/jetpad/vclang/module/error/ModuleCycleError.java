package com.jetbrains.jetpad.vclang.module.error;

import com.jetbrains.jetpad.vclang.module.source.SourceId;

import java.util.List;

public class ModuleCycleError extends ModuleLoadingError {
  public final List<? extends SourceId> cycle;

  public ModuleCycleError(SourceId module, List<? extends SourceId> cycle) {
    super(module, "Module dependencies form a cycle");
    this.cycle = cycle;
  }
}
