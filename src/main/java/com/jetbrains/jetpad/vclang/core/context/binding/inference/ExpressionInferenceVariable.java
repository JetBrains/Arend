package com.jetbrains.jetpad.vclang.core.context.binding.inference;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

import java.util.Set;

public class ExpressionInferenceVariable<T> extends InferenceVariable<T> {
  public ExpressionInferenceVariable(Expression type, Concrete.SourceNode<T> sourceNode, Set<Binding> bounds) {
    super("H", type, sourceNode, bounds);
  }

  @Override
  public LocalTypeCheckingError<T> getErrorInfer(Expression... candidates) {
    return new ArgInferenceError<>(ArgInferenceError.expression(), getSourceNode(), candidates);
  }

  @Override
  public LocalTypeCheckingError<T> getErrorMismatch(Expression expectedType, Expression actualType, Expression candidate) {
    return new ArgInferenceError<>(ArgInferenceError.expression(), expectedType, actualType, getSourceNode(), candidate);
  }
}
