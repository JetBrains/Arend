package com.jetbrains.jetpad.vclang.term.context.binding;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.typechecking.error.LevelInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

public class LevelInferenceBinding extends InferenceBinding {
  private final Abstract.SourceNode mySourceNode;

  public LevelInferenceBinding(String name, Expression type, Abstract.SourceNode sourceNode) {
    super(name, type);
    mySourceNode = sourceNode;
  }

  @Override
  public Abstract.SourceNode getSourceNode() {
    return mySourceNode;
  }

  @Override
  public void reportErrorInfer(ErrorReporter errorReporter, Expression... candidates) {
    errorReporter.report(new LevelInferenceError(mySourceNode));
  }

  @Override
  public void reportErrorMismatch(ErrorReporter errorReporter, Expression expectedType, Expression actualType, Expression candidate) {
    errorReporter.report(new LevelInferenceError(mySourceNode));
  }
}
