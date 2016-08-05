package com.jetbrains.jetpad.vclang.term.context.binding.inference;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;

public class LambdaInferenceVariable extends InferenceVariable {
  private final int myIndex;
  private final Abstract.SourceNode mySourceNode;
  private final boolean myLevel;

  public LambdaInferenceVariable(String name, Expression type, int index, Abstract.SourceNode sourceNode, boolean level) {
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
  public TypeCheckingError getErrorInfer(Expression... candidates) {
    return new ArgInferenceError(myLevel ? ArgInferenceError.levelOfLambdaArg(myIndex) : ArgInferenceError.lambdaArg(myIndex), mySourceNode, candidates);
  }

  @Override
  public TypeCheckingError getErrorMismatch(Expression expectedType, Type actualType, Expression candidate) {
    return new ArgInferenceError(myLevel ? ArgInferenceError.levelOfLambdaArg(myIndex) : ArgInferenceError.lambdaArg(myIndex), expectedType, actualType, mySourceNode, candidate);
  }
}
