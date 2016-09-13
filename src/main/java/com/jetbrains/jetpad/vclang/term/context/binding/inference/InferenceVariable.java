package com.jetbrains.jetpad.vclang.term.context.binding.inference;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Variable;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.InferenceReferenceExpression;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.InferenceVariableListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class InferenceVariable implements Variable {
  private final String myName;
  private Expression myType;
  private InferenceReferenceExpression myReference;
  private List<InferenceVariableListener> myListeners;

  public InferenceVariable(String name, Expression type) {
    myName = name;
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

  public abstract Abstract.SourceNode getSourceNode();

  public abstract LocalTypeCheckingError getErrorInfer(Expression... candidates);

  public abstract LocalTypeCheckingError getErrorMismatch(Expression expectedType, Type actualType, Expression candidate);

  @Override
  public String toString() {
    return "?" + myName;
  }
}
