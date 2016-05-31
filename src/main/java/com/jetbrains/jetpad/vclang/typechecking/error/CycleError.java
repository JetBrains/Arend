package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

public class CycleError extends TypeCheckingError {
  public CycleError(List<Abstract.Definition> cycle) {
    super(cycle.get(0), "Dependency cycle", null);
  }
}
