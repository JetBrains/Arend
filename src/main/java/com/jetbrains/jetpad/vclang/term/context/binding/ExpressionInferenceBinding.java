package com.jetbrains.jetpad.vclang.term.context.binding;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.LevelExpression;
import com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

public class ExpressionInferenceBinding extends InferenceBinding {
  private final Abstract.SourceNode mySourceNode;

  public ExpressionInferenceBinding(Expression type, Abstract.SourceNode sourceNode) {
    super(null, type);
    mySourceNode = sourceNode;
  }

  @Override
  public Abstract.SourceNode getSourceNode() {
    return mySourceNode;
  }

  @Override
  public void reportErrorInfer(ErrorReporter errorReporter, Expression... candidates) {
    errorReporter.report(new ArgInferenceError(ArgInferenceError.expression(), mySourceNode, null, candidates, new LevelExpression[0]));
  }

  @Override
  public void reportErrorLevelInfer(ErrorReporter errorReporter, LevelExpression... candidates) {
    errorReporter.report(new ArgInferenceError(ArgInferenceError.expression(), mySourceNode, null, new Expression[0], candidates));
  }

  @Override
  public void reportErrorMismatch(ErrorReporter errorReporter, Expression expectedType, Expression actualType, Expression candidate) {
    errorReporter.report(new ArgInferenceError(ArgInferenceError.expression(), expectedType, actualType, mySourceNode, null, candidate));
  }
}
