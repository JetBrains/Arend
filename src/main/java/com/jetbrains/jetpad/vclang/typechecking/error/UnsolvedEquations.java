package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.ListEquations;

import java.util.List;

public class UnsolvedEquations extends TypeCheckingError {
  public final List<ListEquations.CmpEquation> equations;
  public final List<ListEquations.LevelCmpEquation> levelEquations;

  public UnsolvedEquations(Abstract.Definition definition, List<ListEquations.CmpEquation> equations, List<ListEquations.LevelCmpEquation> levelEquations) {
    super(definition, "Internal error: some equations were not solved", !equations.isEmpty() ? equations.get(0).sourceNode : levelEquations.get(0).sourceNode);
    this.equations = equations;
    this.levelEquations = levelEquations;
  }

  @Deprecated
  public UnsolvedEquations(List<ListEquations.CmpEquation> equations, List<ListEquations.LevelCmpEquation> levelEquations) {
    this(null, equations, levelEquations);
  }
}
