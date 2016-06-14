package com.jetbrains.jetpad.vclang.term.context.binding;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.LevelExpression;
import com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

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
    errorReporter.report(new ArgInferenceError("Unknown inference variable '" + getName() + "'", null, null, candidates, new LevelExpression[0]));
  }

  @Override
  public void reportErrorLevelInfer(ErrorReporter errorReporter, LevelExpression... candidates) {
    errorReporter.report(new ArgInferenceError("Unknown inference variable '" + getName() + "'", null, null, new Expression[0], candidates));
  }

  @Override
  public void reportErrorMismatch(ErrorReporter errorReporter, Expression expectedType, Expression actualType, Expression candidate) {
    errorReporter.report(new ArgInferenceError("Unknown inference variable '" + getName() + "'", expectedType, actualType, null, null, candidate));
  }
}
