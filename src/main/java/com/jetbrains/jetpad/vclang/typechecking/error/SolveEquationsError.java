package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.LevelEquation;

import java.util.List;

public class SolveEquationsError extends TypeCheckingError {
  public final List<? extends LevelEquation<? extends Binding>> equations;

  public SolveEquationsError(Abstract.Definition definition, List<? extends LevelEquation<? extends Binding>> equations, Abstract.SourceNode cause) {
    super(definition, "Cannot solve equations", cause);
    this.equations = equations;
  }

  public SolveEquationsError(List<? extends LevelEquation<? extends Binding>> equations, Abstract.SourceNode cause) {
    this(null, equations, cause);
  }
}
