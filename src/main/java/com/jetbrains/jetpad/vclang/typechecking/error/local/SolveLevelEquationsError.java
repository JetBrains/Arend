package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Variable;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.LevelEquation;

import java.util.List;

public class SolveLevelEquationsError extends LocalTypeCheckingError {
  public final List<? extends LevelEquation<? extends Variable>> equations;

  public SolveLevelEquationsError(List<? extends LevelEquation<? extends Variable>> equations, Abstract.SourceNode cause) {
    super("Cannot solve equations", cause);
    this.equations = equations;
  }
}
