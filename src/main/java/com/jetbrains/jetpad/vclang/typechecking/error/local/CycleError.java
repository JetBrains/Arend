package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

public class CycleError extends LocalTypeCheckingError {
  public final List<Abstract.Definition> cycle;

  public CycleError(List<Abstract.Definition> cycle) {
    // FIXME[error]: record some reasonable cause
    super("Dependency cycle", null);
    this.cycle = cycle;
  }
}
