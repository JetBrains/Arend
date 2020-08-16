package org.arend.core.context.binding.inference;

import org.arend.core.context.binding.Binding;
import org.arend.core.expr.Expression;
import org.arend.ext.error.LocalError;
import org.arend.ext.error.TypeMismatchError;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.inference.ArgInferenceError;

import java.util.Set;

public class UserInferenceVariable extends InferenceVariable {
  private final boolean mySolvableFromEquations;

  public UserInferenceVariable(String name, Expression type, Concrete.SourceNode sourceNode, Set<Binding> bounds, boolean isSolvableFromEquations) {
    super(name, type, sourceNode, bounds);
    mySolvableFromEquations = isSolvableFromEquations;
  }

  @Override
  public boolean isSolvableFromEquations() {
    return mySolvableFromEquations;
  }

  @Override
  public LocalError getErrorInfer(Expression... candidates) {
    return new ArgInferenceError("Cannot infer variable '" + getName() + "'", getSourceNode(), candidates);
  }

  @Override
  public LocalError getErrorMismatch(Expression expectedType, Expression actualType, Expression candidate) {
    return new TypeMismatchError(expectedType, actualType, getSourceNode());
  }
}
