package org.arend.core.context.binding.inference;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.Variable;
import org.arend.core.expr.Expression;
import org.arend.core.expr.InferenceReferenceExpression;
import org.arend.core.subst.ExprSubstitution;
import org.arend.ext.core.context.CoreInferenceVariable;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.LocalError;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.arend.typechecking.implicitargs.equations.InferenceVariableListener;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class InferenceVariable implements Variable, CoreInferenceVariable {
  private final String myName;
  private final Concrete.SourceNode mySourceNode;
  private Expression myType;
  private InferenceReferenceExpression myReference;
  private List<InferenceVariableListener> myListeners;
  private final Set<Binding> myBounds;
  private boolean mySolved;
  private ExprSubstitution mySubstitution;

  public InferenceVariable(String name, Expression type, Concrete.SourceNode sourceNode, Set<Binding> bounds) {
    myName = name == null || name.isEmpty() ? "x" : name;
    mySourceNode = sourceNode;
    myType = type;
    myListeners = Collections.emptyList();
    myBounds = bounds;
  }

  public Set<Binding> getBounds() {
    return myBounds;
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

  public void solve(Equations equations, Expression solution) {
    if (mySolved) {
      return;
    }
    mySolved = true;
    myReference.setSubstExpression(mySubstitution == null ? solution : solution.subst(mySubstitution));
    mySubstitution = null;
    for (InferenceVariableListener listener : myListeners) {
      listener.solved(equations, myReference);
    }
  }

  public boolean isSolved() {
    return mySolved;
  }

  public Expression getSolution() {
    return myReference == null ? null : myReference.getSubstExpression();
  }

  @Nonnull
  @Override
  public String getName() {
    return myName;
  }

  public Concrete.SourceNode getSourceNode() {
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

  public abstract LocalError getErrorInfer(Expression... candidates);

  public abstract LocalError getErrorMismatch(Expression expectedType, Expression actualType, Expression candidate);

  @Override
  public String toString() {
    return "?" + myName;
  }
}
