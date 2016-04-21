package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.context.binding.InferenceBinding;
import com.jetbrains.jetpad.vclang.term.expr.Substitution;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class TypeCheckingResult {
  private Equations myEquations;
  private Set<InferenceBinding> myUnsolvedVariables;

  public TypeCheckingResult() {
    myEquations = DummyEquations.getInstance();
    myUnsolvedVariables = Collections.emptySet();
  }

  public Equations getEquations() {
    return myEquations;
  }

  public void setEquations(Equations equations) {
    myEquations = equations;
  }

  public void addUnsolvedVariable(InferenceBinding binding) {
    if (myUnsolvedVariables.isEmpty()) {
      myUnsolvedVariables = new HashSet<>();
    }
    myUnsolvedVariables.add(binding);
  }

  public boolean removeUnsolvedVariable(InferenceBinding binding) {
    return myUnsolvedVariables.remove(binding);
  }

  public void reportErrors(ErrorReporter errorReporter) {
    if (myUnsolvedVariables.isEmpty()) {
      myEquations.reportErrors(errorReporter);
    }
    for (InferenceBinding unsolvedVariable : myUnsolvedVariables) {
      unsolvedVariable.reportErrorInfer(errorReporter);
    }
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

  public Substitution getSubstitution(boolean onlyPreciseSolutions) {
    if (!myEquations.isEmpty()) {
      return myEquations.getInferenceVariables(myUnsolvedVariables, onlyPreciseSolutions);
    } else {
      return new Substitution();
    }
  }

  public void update(boolean onlyPreciseSolutions) {
    if (!myEquations.isEmpty()) {
      subst(myEquations.getInferenceVariables(myUnsolvedVariables, onlyPreciseSolutions));
    }
  }

  public abstract void subst(Substitution substitution);
}
