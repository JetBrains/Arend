package com.jetbrains.jetpad.vclang.term.context.binding;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.LevelExpression;
import com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

public class FunctionInferenceBinding extends InferenceBinding {
  private final int myIndex;
  private final Abstract.SourceNode mySourceNode;

  public FunctionInferenceBinding(String name, Expression type, int index, Abstract.SourceNode sourceNode) {
    super(name, type);
    myIndex = index;
    mySourceNode = sourceNode;
  }

  @Override
  public Abstract.SourceNode getSourceNode() {
    return mySourceNode;
  }

  @Override
  public void reportErrorInfer(ErrorReporter errorReporter, Expression... candidates) {
    errorReporter.report(new ArgInferenceError(ArgInferenceError.functionArg(myIndex), mySourceNode, null, candidates, new LevelExpression[0]));
  }

  @Override
  public void reportErrorLevelInfer(ErrorReporter errorReporter, LevelExpression... candidates) {
    errorReporter.report(new ArgInferenceError(ArgInferenceError.functionArg(myIndex), mySourceNode, null, new Expression[0], candidates));
  }

  @Override
  public void reportErrorMismatch(ErrorReporter errorReporter, Expression expectedType, Expression actualType, Expression candidate) {
    errorReporter.report(new ArgInferenceError(ArgInferenceError.functionArg(myIndex), expectedType, actualType, mySourceNode, null, candidate));
  }
}
