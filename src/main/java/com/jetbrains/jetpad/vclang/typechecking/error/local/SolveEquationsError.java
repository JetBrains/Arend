package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equation;

import java.util.List;

public class SolveEquationsError extends LocalTypeCheckingError {
  public final List<? extends Equation> equations;

  public SolveEquationsError(List<? extends Equation> equations, Abstract.SourceNode cause) {
    super("Cannot solve equations", cause);
    this.equations = equations;
  }
}
