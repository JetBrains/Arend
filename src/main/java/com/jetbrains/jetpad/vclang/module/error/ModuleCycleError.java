package com.jetbrains.jetpad.vclang.module.error;

import com.jetbrains.jetpad.vclang.module.ModuleID;

import java.util.Collection;
import java.util.List;

public class ModuleCycleError extends ModuleLoadingError {
  public final List<ModuleID> cycle;

  public ModuleCycleError(List<ModuleID> cycle) {
    super(cycle.get(0), "Module dependencies form a cycle");
    this.cycle = cycle;
  }
}
