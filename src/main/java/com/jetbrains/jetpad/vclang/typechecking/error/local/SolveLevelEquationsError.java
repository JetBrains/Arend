package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.LevelEquation;

import java.util.List;

public class SolveLevelEquationsError extends LocalTypeCheckingError {
  public final List<? extends LevelEquation<? extends LevelVariable>> equations;

  public SolveLevelEquationsError(List<? extends LevelEquation<? extends LevelVariable>> equations, Abstract.SourceNode cause) {
    super("Cannot solve equations", cause);
    this.equations = equations;
  }
}
