package com.jetbrains.jetpad.vclang.core.context.binding.inference;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

public class DerivedInferenceVariable extends InferenceVariable {
  private final InferenceVariable myVar;

  public DerivedInferenceVariable(String name, InferenceVariable binding, Type type) {
    super(name, type, binding.getSourceNode());
    myVar = binding;
  }

  @Override
  public LocalTypeCheckingError getErrorInfer(Expression... candidates) {
    return myVar.getErrorInfer(candidates);
  }

  @Override
  public LocalTypeCheckingError getErrorMismatch(Expression expectedType, Expression actualType, Expression candidate) {
    return myVar.getErrorMismatch(expectedType, actualType, candidate);
  }
}
