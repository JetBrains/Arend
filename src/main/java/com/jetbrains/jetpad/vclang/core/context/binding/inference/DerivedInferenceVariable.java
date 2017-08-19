package com.jetbrains.jetpad.vclang.core.context.binding.inference;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

import java.util.Set;

public class DerivedInferenceVariable<T> extends InferenceVariable<T> {
  private final InferenceVariable<T> myVar;

  public DerivedInferenceVariable(String name, InferenceVariable<T> binding, Expression type, Set<Binding> bounds) {
    super(name, type, binding.getSourceNode(), bounds);
    myVar = binding;
  }

  @Override
  public LocalTypeCheckingError<T> getErrorInfer(Expression... candidates) {
    return myVar.getErrorInfer(candidates);
  }

  @Override
  public LocalTypeCheckingError<T> getErrorMismatch(Expression expectedType, Expression actualType, Expression candidate) {
    return myVar.getErrorMismatch(expectedType, actualType, candidate);
  }
}
