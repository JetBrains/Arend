package com.jetbrains.jetpad.vclang.term.context.binding;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

public abstract class InferenceBinding extends TypedBinding {
  public InferenceBinding(String name, Expression type) {
    super(name, type);
  }

  public abstract Abstract.SourceNode getSourceNode();

  public abstract void reportError(ErrorReporter errorReporter, Expression... candidates);

  public void setType(Expression type) {
    myType = type;
  }
}
