package com.jetbrains.jetpad.vclang.term.context.binding.inference;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

public class FunctionInferenceVariable extends InferenceVariable {
  private final int myIndex;
  private final Abstract.SourceNode mySourceNode;

  public FunctionInferenceVariable(String name, Expression type, int index, Abstract.SourceNode sourceNode) {
    super(name, type);
    myIndex = index;
    mySourceNode = sourceNode;
  }

  @Override
  public Abstract.SourceNode getSourceNode() {
    return mySourceNode;
  }

  @Override
  public LocalTypeCheckingError getErrorInfer(Expression... candidates) {
    return new ArgInferenceError(ArgInferenceError.functionArg(myIndex), mySourceNode, candidates);
  }

  @Override
  public LocalTypeCheckingError getErrorMismatch(Expression expectedType, Type actualType, Expression candidate) {
    return new ArgInferenceError(ArgInferenceError.functionArg(myIndex), expectedType, actualType, mySourceNode, candidate);
  }
}
