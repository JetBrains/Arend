package com.jetbrains.jetpad.vclang.term.context.binding.inference;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;

public class TypeClassInferenceVariable extends InferenceVariable {
  public TypeClassInferenceVariable(String name, Expression type, Abstract.SourceNode sourceNode) {
    super(name, type, sourceNode);
  }

  @Override
  public TypeCheckingError getErrorInfer(Expression... candidates) {
    return null;
  }

  @Override
  public TypeCheckingError getErrorMismatch(Expression expectedType, Type actualType, Expression candidate) {
    return null;
  }
}
