package com.jetbrains.jetpad.vclang.term.context.binding;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;

public class IgnoreBinding extends InferenceBinding {
  public IgnoreBinding(String name, Expression type) {
    super(name, type);
  }

  @Override
  public Abstract.SourceNode getSourceNode() {
    return null;
  }

  @Override
  public void reportError(ErrorReporter errorReporter, Expression... candidates) {

  }
}
