package com.jetbrains.jetpad.vclang.term.context.binding;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError;

public class UnknownInferenceBinding extends InferenceBinding {
  public UnknownInferenceBinding(String name, Expression type) {
    super(name, type);
  }

  @Override
  public Abstract.SourceNode getSourceNode() {
    return null;
  }

  @Override
  public void reportErrorInfer(ErrorReporter errorReporter, Expression... candidates) {
    errorReporter.report(new ArgInferenceError("Unknown inference variable '" + getName() + "'", null, candidates));
  }

  @Override
  public void reportErrorLevelInfer(ErrorReporter errorReporter, Level... candidates) {
    throw new IllegalStateException();
    //errorReporter.report(new ArgInferenceError("Unknown inference variable '" + getName() + "'", null, new Expression[0], candidates));
  }

  @Override
  public void reportErrorMismatch(ErrorReporter errorReporter, Expression expectedType, Type actualType, Expression candidate) {
    errorReporter.report(new ArgInferenceError("Unknown inference variable '" + getName() + "'", expectedType, actualType, null, candidate));
  }
}
