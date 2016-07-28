package com.jetbrains.jetpad.vclang.term.context.binding.inference;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError;

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
    errorReporter.report(new ArgInferenceError(myLevel ? ArgInferenceError.levelOfLambdaArg(myIndex) : ArgInferenceError.lambdaArg(myIndex), mySourceNode, candidates));
  }

  @Override
  public void reportErrorLevelInfer(ErrorReporter errorReporter, Level... candidates) {
    throw new IllegalStateException();
    //errorReporter.report(new ArgInferenceError(myLevel ? ArgInferenceError.levelOfLambdaArg(myIndex) : ArgInferenceError.lambdaArg(myIndex), mySourceNode, new Expression[0]));
  }

  @Override
  public void reportErrorMismatch(ErrorReporter errorReporter, Expression expectedType, Type actualType, Expression candidate) {
    errorReporter.report(new ArgInferenceError(myLevel ? ArgInferenceError.levelOfLambdaArg(myIndex) : ArgInferenceError.lambdaArg(myIndex), expectedType, actualType, mySourceNode, candidate));
  }
}
