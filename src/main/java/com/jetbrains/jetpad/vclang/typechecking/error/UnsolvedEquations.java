package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.ListEquations;

import java.util.List;

public class UnsolvedEquations extends TypeCheckingError {
  public final List<ListEquations.CmpEquation> equations;

  public UnsolvedEquations(Abstract.Definition definition, List<ListEquations.CmpEquation> equations) {
    super(definition, "Internal error: some equations were not solved", equations.get(0).sourceNode);
    this.equations = equations;
  }

  @Deprecated
  public UnsolvedEquations(List<ListEquations.CmpEquation> equations) {
    this(null, equations);
  }
}
