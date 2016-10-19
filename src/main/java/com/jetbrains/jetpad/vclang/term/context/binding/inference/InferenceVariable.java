package com.jetbrains.jetpad.vclang.term.context.binding.inference;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Variable;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.InferenceReferenceExpression;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.term.expr.type.TypeMax;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.InferenceVariableListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class InferenceVariable implements Variable {
  private final String myName;
  private final Abstract.SourceNode mySourceNode;
  private Type myType;
  private InferenceReferenceExpression myReference;
  private List<InferenceVariableListener> myListeners;

  public InferenceVariable(String name, Type type, Abstract.SourceNode sourceNode) {
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

  @Override
  public String getName() {
    return myName;
  }

  public Abstract.SourceNode getSourceNode() {
    return mySourceNode;
  }

  @Override
  public Type getType() {
    return myType;
  }

  public void setType(Type type) {
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

  public abstract LocalTypeCheckingError getErrorMismatch(Type expectedType, TypeMax actualType, Expression candidate);

  @Override
  public String toString() {
    return "?" + myName;
  }
}
