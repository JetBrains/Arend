package com.jetbrains.jetpad.vclang.term.context.binding;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

public class IgnoreBinding extends InferenceBinding {
  public IgnoreBinding(String name, Expression type) {
    super(name, type);
  }

  @Override
  public Abstract.SourceNode getSourceNode() {
    return null;
  }

  @Override
  public void reportErrorInfer(ErrorReporter errorReporter, Expression... candidates) {

  }

  @Override
  public void reportErrorMismatch(ErrorReporter errorReporter, Expression expectedType, Expression actualType, Expression candidate) {

  }
}
