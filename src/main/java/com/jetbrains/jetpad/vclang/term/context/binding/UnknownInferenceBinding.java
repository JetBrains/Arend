package com.jetbrains.jetpad.vclang.term.context.binding;

import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

public class UnknownInferenceBinding extends InferenceBinding {
  public UnknownInferenceBinding(String name, Expression type) {
    super(name, type);
  }

  @Override
  public void reportError(ErrorReporter errorReporter, Expression... candidates) {
    errorReporter.report(new ArgInferenceError("Unknown inference variable '" + getName() + "'", null, null, candidates));
  }
}
