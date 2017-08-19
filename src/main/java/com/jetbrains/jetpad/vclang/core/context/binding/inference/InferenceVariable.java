package com.jetbrains.jetpad.vclang.core.context.binding.inference;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.InferenceReferenceExpression;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.InferenceVariableListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class InferenceVariable<T> implements Variable {
  private final String myName;
  private final Concrete.SourceNode<T> mySourceNode;
  private Expression myType;
  private InferenceReferenceExpression myReference;
  private List<InferenceVariableListener<T>> myListeners;
  private final Set<Binding> myBounds;

  public InferenceVariable(String name, Expression type, Concrete.SourceNode<T> sourceNode, Set<Binding> bounds) {
    myName = name;
    mySourceNode = sourceNode;
    myType = type;
    myListeners = Collections.emptyList();
    myBounds = bounds;
  }

  public Set<Binding> getBounds() {
    return myBounds;
  }

  public void addListener(InferenceVariableListener<T> listener) {
    if (myListeners.isEmpty()) {
      myListeners = new ArrayList<>(3);
    }
    myListeners.add(listener);
  }

  public void removeListener(InferenceVariableListener<T> listener) {
    if (!myListeners.isEmpty()) {
      myListeners.remove(listener);
    }
  }

  public void solve(Equations<T> equations, Expression solution) {
    if (myReference != null) {
      myReference.setSubstExpression(solution);
      for (InferenceVariableListener<T> listener : myListeners) {
        listener.solved(equations, myReference);
      }
      myReference = null;
    }
  }

  public boolean isSolved() {
    return myReference == null;
  }

  @Override
  public String getName() {
    return myName;
  }

  public Concrete.SourceNode<T> getSourceNode() {
    return mySourceNode;
  }

  public Expression getType() {
    return myType;
  }

  public void setType(Expression type) {
    myType = type;
  }

  public void setReference(InferenceReferenceExpression reference) {
    if (myReference == null) {
      myReference = reference;
    } else {
      throw new IllegalStateException();
    }
  }

  public abstract LocalTypeCheckingError<T> getErrorInfer(Expression... candidates);

  public abstract LocalTypeCheckingError<T> getErrorMismatch(Expression expectedType, Expression actualType, Expression candidate);

  @Override
  public String toString() {
    return "?" + myName;
  }
}
