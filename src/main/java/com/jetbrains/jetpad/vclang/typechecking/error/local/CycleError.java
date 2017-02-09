package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

public class CycleError extends GeneralError {
  public final List<Abstract.Definition> cycle;

  public CycleError(List<Abstract.Definition> cycle) {
    super("Dependency cycle", cycle.get(0));
    this.cycle = cycle;
  }
}
