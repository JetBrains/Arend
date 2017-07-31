package com.jetbrains.jetpad.vclang.core.context.binding.inference;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

import java.util.Set;

public class LambdaInferenceVariable extends InferenceVariable {
  private final int myIndex;
  private final boolean myLevel;

  public LambdaInferenceVariable(String name, Expression type, int index, Abstract.SourceNode sourceNode, boolean level, Set<Binding> bounds) {
    super(name, type, sourceNode, bounds);
    myIndex = index;
    myLevel = level;
  }

  @Override
  public LocalTypeCheckingError getErrorInfer(Expression... candidates) {
    return new ArgInferenceError(myLevel ? ArgInferenceError.levelOfLambdaArg(myIndex) : ArgInferenceError.lambdaArg(myIndex), getSourceNode(), candidates);
  }

  @Override
  public LocalTypeCheckingError getErrorMismatch(Expression expectedType, Expression actualType, Expression candidate) {
    return new ArgInferenceError(myLevel ? ArgInferenceError.levelOfLambdaArg(myIndex) : ArgInferenceError.lambdaArg(myIndex), expectedType, actualType, getSourceNode(), candidate);
  }
}
