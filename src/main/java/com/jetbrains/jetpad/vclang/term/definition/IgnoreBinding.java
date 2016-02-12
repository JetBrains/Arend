package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

public class IgnoreBinding extends InferenceBinding {
  public IgnoreBinding(String name, Expression type) {
    super(name, type);
  }

  @Override
  public void reportError(ErrorReporter errorReporter, Expression... candidates) {

  }
}
