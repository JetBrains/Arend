package org.arend.core.context.binding.inference;

import org.arend.core.context.binding.Binding;
import org.arend.core.expr.Expression;
import org.arend.core.expr.InferenceReferenceExpression;
import org.arend.ext.core.context.CoreInferenceVariable;
import org.arend.ext.variable.Variable;
import org.arend.naming.renamer.Renamer;
import org.arend.term.concrete.Concrete;
import org.arend.ext.error.LocalError;
import org.arend.typechecking.implicitargs.equations.InferenceVariableListener;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class InferenceVariable implements Variable, CoreInferenceVariable {
  private final String myName;
  private Expression myType;
  private final Set<Binding> myBounds;
  private final Concrete.SourceNode mySourceNode;
  private InferenceReferenceExpression myReference;
  private List<InferenceVariableListener> myListeners;

  public InferenceVariable(String name, Expression type, Concrete.SourceNode sourceNode, Set<Binding> bounds) {
    myName = name == null || name.isEmpty() ? Renamer.UNNAMED : name;
    myType = type;
    myBounds = bounds;
    mySourceNode = sourceNode;
    myListeners = Collections.emptyList();
  }

  @NotNull
  @Override
  public String getName() {
    return myName;
  }

  public Expression getType() {
    return myType;
  }

  public void setType(Expression type) {
    myType = type;
  }

  public Set<Binding> getBounds() {
    return myBounds;
  }

  @Override
  public String toString() {
    return "?" + getName();
  }

  public boolean isSolvableFromEquations() {
    return true;
  }

  public void addListener(InferenceVariableListener listener) {
    if (myListeners.isEmpty()) {
      myListeners = new ArrayList<>(3);
    }
    myListeners.add(listener);
  }

  public void removeListener(InferenceVariableListener listener) {
    if (!myListeners.isEmpty()) {
      myListeners.remove(listener);
    }
  }

  public void solve(CheckTypeVisitor checker, Expression solution) {
    if (isSolved()) {
      return;
    }
    myReference.setSubstExpression(solution);
    checker.variableSolved(this);
    for (InferenceVariableListener listener : myListeners) {
      listener.solved(checker.getEquations(), myReference);
    }
  }

  public void unsolve() {
    myReference.setSubstExpression(null);
  }

  public boolean isSolved() {
    return myReference.getVariable() != this;
  }

  public Expression getSolution() {
    return myReference == null ? null : myReference.getSubstExpression();
  }

  public Concrete.SourceNode getSourceNode() {
    return mySourceNode;
  }

  public void setReference(InferenceReferenceExpression reference) {
    if (myReference == null) {
      myReference = reference;
    } else {
      throw new IllegalStateException();
    }
  }

  public abstract LocalError getErrorInfer(Expression... candidates);

  public abstract LocalError getErrorMismatch(Expression expectedType, Expression actualType, Expression candidate);
}
