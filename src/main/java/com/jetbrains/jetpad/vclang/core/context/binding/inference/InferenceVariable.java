package com.jetbrains.jetpad.vclang.core.context.binding.inference;

import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.InferenceReferenceExpression;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.InferenceVariableListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class InferenceVariable implements Variable {
  private final String myName;
  private final Abstract.SourceNode mySourceNode;
  private Expression myType;
  private InferenceReferenceExpression myReference;
  private List<InferenceVariableListener> myListeners;

  public InferenceVariable(String name, Expression type, Abstract.SourceNode sourceNode) {
    myName = name;
    mySourceNode = sourceNode;
    myType = type;
    myListeners = Collections.emptyList();
  }

  public void addListener(InferenceVariableListener listener) {
    if (myListeners.isEmpty()) {
      myListeners = new ArrayList<>(3);
    }
    myListeners.add(listener);
  }

  public void solve(Equations equations, Expression solution) {
    if (myReference != null) {
      myReference.setSubstExpression(solution);
      for (InferenceVariableListener listener : myListeners) {
        listener.solved(equations, myReference);
      }
      myReference = null;
    }
  }

  public boolean isSolved() {
    return myReference == null;
  }

  public String getName() {
    return myName;
  }

  public Abstract.SourceNode getSourceNode() {
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

  public abstract LocalTypeCheckingError getErrorInfer(Expression... candidates);

  public abstract LocalTypeCheckingError getErrorMismatch(Expression expectedType, Expression actualType, Expression candidate);

  @Override
  public String toString() {
    return "?" + myName;
  }
}
