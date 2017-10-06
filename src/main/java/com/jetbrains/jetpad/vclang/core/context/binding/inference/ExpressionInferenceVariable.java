package com.jetbrains.jetpad.vclang.core.context.binding.inference;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalError;

import java.util.Set;

public class ExpressionInferenceVariable extends InferenceVariable {
  public ExpressionInferenceVariable(Expression type, Concrete.SourceNode sourceNode, Set<Binding> bounds) {
    super("H", type, sourceNode, bounds);
  }

  @Override
  public LocalError getErrorInfer(Expression... candidates) {
    return new ArgInferenceError(ArgInferenceError.expression(), getSourceNode(), candidates);
  }

  @Override
  public LocalError getErrorMismatch(Expression expectedType, Expression actualType, Expression candidate) {
    return new ArgInferenceError(ArgInferenceError.expression(), expectedType, actualType, getSourceNode(), candidate);
  }
}
