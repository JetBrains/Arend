package org.arend.core.context.binding.inference;

import org.arend.core.context.binding.Binding;
import org.arend.core.expr.Expression;
import org.arend.typechecking.error.local.LocalError;

import java.util.Set;

public class DerivedInferenceVariable extends InferenceVariable {
  private final InferenceVariable myVar;

  public DerivedInferenceVariable(String name, InferenceVariable binding, Expression type, Set<Binding> bounds) {
    super(name, type, binding.getSourceNode(), bounds);
    myVar = binding;
  }

  @Override
  public LocalError getErrorInfer(Expression... candidates) {
    return myVar.getErrorInfer(candidates);
  }

  @Override
  public LocalError getErrorMismatch(Expression expectedType, Expression actualType, Expression candidate) {
    return myVar.getErrorMismatch(expectedType, actualType, candidate);
  }
}
