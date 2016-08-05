package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.LevelInferenceVariable;
import com.jetbrains.jetpad.vclang.term.expr.subst.Substitution;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.LinkedHashSet;
import java.util.Set;

public abstract class TypeCheckingResult {
  private Equations myEquations;
  private Set<InferenceVariable> myUnsolvedVariables;

  public TypeCheckingResult() {
    myEquations = DummyEquations.getInstance();
    myUnsolvedVariables = new LinkedHashSet<>();
  }

  public Equations getEquations() {
    return myEquations;
  }

  public void setEquations(Equations equations) {
    myEquations = equations;
  }

  public void addUnsolvedVariable(InferenceVariable binding) {
    if (binding instanceof LevelInferenceVariable) {
      myEquations.addVariable((LevelInferenceVariable) binding);
    } else {
      myUnsolvedVariables.add(binding);
    }
  }

  public boolean hasUnsolvedVariables() {
    return !myUnsolvedVariables.isEmpty();
  }

  public void reportErrors(ErrorReporter errorReporter) {
    // TODO[inf_vars]
    // if (myUnsolvedVariables.isEmpty()) {
      myEquations.reportErrors(errorReporter);
    // }
    // for (InferenceVariable unsolvedVariable : myUnsolvedVariables) {
    //   unsolvedVariable.reportErrorInfer(errorReporter);
    // }
  }

  public void add(TypeCheckingResult result) {
    if (myEquations.isEmpty()) {
      myEquations = result.myEquations;
    } else {
      myEquations.add(result.myEquations);
    }

    if (myUnsolvedVariables.isEmpty()) {
      myUnsolvedVariables = result.myUnsolvedVariables;
    } else {
      myUnsolvedVariables.addAll(result.myUnsolvedVariables);
    }
  }

  public Substitution getSubstitution() {
    if (!myEquations.isEmpty()) {
      return myEquations.solve(myUnsolvedVariables, false);
    } else {
      return new Substitution();
    }
  }

  public void update(boolean isFinal) {
    if (!myEquations.isEmpty()) {
      subst(myEquations.solve(myUnsolvedVariables, isFinal));
    }
  }

  public abstract void subst(Substitution substitution);
}
