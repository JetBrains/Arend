package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Variable;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.LevelEquation;

import java.util.List;

public class SolveLevelEquationsError extends TypeCheckingError {
  public final List<? extends LevelEquation<? extends Variable>> equations;

  public SolveLevelEquationsError(Abstract.Definition definition, List<? extends LevelEquation<? extends Variable>> equations, Abstract.SourceNode cause) {
    super(definition, "Cannot solve equations", cause);
    this.equations = equations;
  }

  public SolveLevelEquationsError(List<? extends LevelEquation<? extends Variable>> equations, Abstract.SourceNode cause) {
    this(null, equations, cause);
  }
}
