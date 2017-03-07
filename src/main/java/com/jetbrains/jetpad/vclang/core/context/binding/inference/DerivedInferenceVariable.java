package com.jetbrains.jetpad.vclang.core.context.binding.inference;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeMax;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

public class DerivedInferenceVariable extends InferenceVariable {
  private final InferenceVariable myVar;

  public DerivedInferenceVariable(String name, InferenceVariable binding, Expression type) {
    super(name, type, binding.getSourceNode());
    myVar = binding;
  }

  @Override
  public LocalTypeCheckingError getErrorInfer(Expression... candidates) {
    return myVar.getErrorInfer(candidates);
  }

  @Override
  public LocalTypeCheckingError getErrorMismatch(Type expectedType, TypeMax actualType, Expression candidate) {
    return myVar.getErrorMismatch(expectedType, actualType, candidate);
  }
}
