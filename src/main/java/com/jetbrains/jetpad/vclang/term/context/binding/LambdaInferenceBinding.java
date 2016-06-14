package com.jetbrains.jetpad.vclang.term.context.binding;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.LevelExpression;
import com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

public class LambdaInferenceBinding extends InferenceBinding {
  private final int myIndex;
  private final Abstract.SourceNode mySourceNode;
  private final boolean myLevel;

  public LambdaInferenceBinding(String name, Expression type, int index, Abstract.SourceNode sourceNode, boolean level) {
    super(name, type);
    myIndex = index;
    mySourceNode = sourceNode;
    myLevel = level;
  }

  @Override
  public Abstract.SourceNode getSourceNode() {
    return mySourceNode;
  }

  @Override
  public void reportErrorInfer(ErrorReporter errorReporter, Expression... candidates) {
    errorReporter.report(new ArgInferenceError(myLevel ? ArgInferenceError.levelOfLambdaArg(myIndex) : ArgInferenceError.lambdaArg(myIndex), mySourceNode, null, candidates, new LevelExpression[0]));
  }

  @Override
  public void reportErrorLevelInfer(ErrorReporter errorReporter, LevelExpression... candidates) {
    errorReporter.report(new ArgInferenceError(myLevel ? ArgInferenceError.levelOfLambdaArg(myIndex) : ArgInferenceError.lambdaArg(myIndex), mySourceNode, null, new Expression[0], candidates));
  }

  @Override
  public void reportErrorMismatch(ErrorReporter errorReporter, Expression expectedType, Expression actualType, Expression candidate) {
    errorReporter.report(new ArgInferenceError(myLevel ? ArgInferenceError.levelOfLambdaArg(myIndex) : ArgInferenceError.lambdaArg(myIndex), expectedType, actualType, mySourceNode, null, candidate));
  }
}
