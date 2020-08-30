package org.arend.typechecking;

import org.arend.core.context.binding.inference.InferenceVariable;
import org.arend.typechecking.implicitargs.equations.Equation;
import org.arend.typechecking.visitor.CheckTypeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TypecheckerState {
  public final CheckTypeVisitor.MyErrorReporter errorReporter;
  public final int numberOfDeferredMetasBeforeSolver;
  public final int numberOfDeferredMetasBeforeLevels;
  public final int numberOfDeferredMetasAfterLevels;
  public final TypecheckerState previousState;
  public final List<InferenceVariable> solvedVariables = new ArrayList<>();
  public List<Equation> equations;
  public int numberOfLevelVariables;
  public int numberOfLevelEquations;
  public int numberOfProps;
  public int numberOfBoundVars;
  public Set<InferenceVariable> notSolvableFromEquationsVars;

  public TypecheckerState(CheckTypeVisitor.MyErrorReporter errorReporter, int numberOfDeferredMetasBeforeSolver, int numberOfDeferredMetasBeforeLevels, int numberOfDeferredMetasAfterLevels, TypecheckerState previousState) {
    this.errorReporter = errorReporter;
    this.numberOfDeferredMetasBeforeSolver = numberOfDeferredMetasBeforeSolver;
    this.numberOfDeferredMetasBeforeLevels = numberOfDeferredMetasBeforeLevels;
    this.numberOfDeferredMetasAfterLevels = numberOfDeferredMetasAfterLevels;
    this.previousState = previousState;
  }
}
