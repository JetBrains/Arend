package com.jetbrains.jetpad.vclang.term.context.binding.inference;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;

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
  public TypeCheckingError getErrorInfer(Expression... candidates) {
    return new ArgInferenceError(ArgInferenceError.functionArg(myIndex), mySourceNode, candidates);
  }

  @Override
  public TypeCheckingError getErrorMismatch(Expression expectedType, Type actualType, Expression candidate) {
    return new ArgInferenceError(ArgInferenceError.functionArg(myIndex), expectedType, actualType, mySourceNode, candidate);
  }
}
