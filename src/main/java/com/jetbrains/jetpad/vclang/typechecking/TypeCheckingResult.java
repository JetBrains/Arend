package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.LevelInferenceVariable;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

public abstract class TypeCheckingResult {
  private final Equations myEquations;

  public TypeCheckingResult(Equations equations) {
    myEquations = equations;
  }

  public Equations getEquations() {
    return myEquations;
  }

  public void addLevelVariable(LevelInferenceVariable var) {
    myEquations.addVariable(var);
  }

  public void reportErrors(ErrorReporter errorReporter, Abstract.SourceNode sourceNode) {
    myEquations.reportErrors(errorReporter, sourceNode);
  }

  public void add(TypeCheckingResult result) {
    myEquations.add(result.myEquations);
  }

  public void solve() {
    if (!myEquations.isEmpty()) {
      subst(myEquations.solve());
    }
  }

  public abstract void subst(LevelSubstitution substitution);
}
