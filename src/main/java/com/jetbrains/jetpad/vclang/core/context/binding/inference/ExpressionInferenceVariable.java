package com.jetbrains.jetpad.vclang.core.context.binding.inference;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

import java.util.Set;

public class ExpressionInferenceVariable extends InferenceVariable {
  public ExpressionInferenceVariable(Expression type, Abstract.SourceNode sourceNode, Set<Binding> bounds) {
    super("H", type, sourceNode, bounds);
  }

  @Override
  public LocalTypeCheckingError getErrorInfer(Expression... candidates) {
    return new ArgInferenceError(ArgInferenceError.expression(), (Concrete.SourceNode) getSourceNode(), candidates);
  }

  @Override
  public LocalTypeCheckingError getErrorMismatch(Expression expectedType, Expression actualType, Expression candidate) {
    return new ArgInferenceError(ArgInferenceError.expression(), expectedType, actualType, (Concrete.SourceNode) getSourceNode(), candidate);
  }
}
