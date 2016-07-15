package com.jetbrains.jetpad.vclang.term.context.binding;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;


public abstract class InferenceBinding extends TypedBinding {
  public InferenceBinding(String name, Expression type) {
    super(name, type);
  }

  public void setType(Expression type) {
    myType = type;
  }

  public abstract Abstract.SourceNode getSourceNode();

  public abstract void reportErrorInfer(ErrorReporter errorReporter, Expression... candidates);
  public abstract void reportErrorLevelInfer(ErrorReporter errorReporter, Level... candidates);
  public abstract void reportErrorMismatch(ErrorReporter errorReporter, Expression expectedType, Type actualType, Expression candidate);
}
