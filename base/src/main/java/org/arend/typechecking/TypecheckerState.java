package org.arend.typechecking;

import org.arend.core.context.binding.inference.InferenceVariable;
import org.arend.ext.error.ListErrorReporter;
import org.arend.extImpl.userData.UserDataHolderImpl;
import org.arend.typechecking.implicitargs.equations.Equation;
import org.arend.typechecking.visitor.CheckTypeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TypecheckerState {
  public final CheckTypeVisitor.MyErrorReporter errorReporter;
  public final int numberOfDeferredMetasBeforeSolver;
  public final int numberOfDeferredMetasAfterLevels;
  public final TypecheckerState previousState;
  public final List<InferenceVariable> solvedVariables = new ArrayList<>();
  public List<Equation> equations;
  public int numberOfLevelVariables;
  public int numberOfLevelEquations;
  public int numberOfProps;
  public int numberOfBoundVars;
  public final ListErrorReporter listErrorReporter;
  public Set<InferenceVariable> notSolvableFromEquationsVars;
  public UserDataHolderImpl userDataHolder;
  public final boolean allowDeferredMetas;

  public TypecheckerState(CheckTypeVisitor.MyErrorReporter errorReporter, int numberOfDeferredMetasBeforeSolver, int numberOfDeferredMetasAfterLevels, UserDataHolderImpl userDataHolder, TypecheckerState previousState, ListErrorReporter listErrorReporter, boolean allowDeferredMetas) {
    this.errorReporter = errorReporter;
    this.numberOfDeferredMetasBeforeSolver = numberOfDeferredMetasBeforeSolver;
    this.numberOfDeferredMetasAfterLevels = numberOfDeferredMetasAfterLevels;
    this.userDataHolder = userDataHolder;
    this.previousState = previousState;
    this.listErrorReporter = listErrorReporter;
    this.allowDeferredMetas = allowDeferredMetas;
  }
}
