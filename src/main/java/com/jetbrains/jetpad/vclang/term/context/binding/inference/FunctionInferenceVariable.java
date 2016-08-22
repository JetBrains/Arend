package com.jetbrains.jetpad.vclang.term.context.binding.inference;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;

public class FunctionInferenceVariable extends InferenceVariable {
  private final int myIndex;

  public FunctionInferenceVariable(String name, Expression type, int index, Abstract.SourceNode sourceNode) {
    super(name, type, sourceNode);
    myIndex = index;
  }

  @Override
  public TypeCheckingError getErrorInfer(Expression... candidates) {
    return new ArgInferenceError(ArgInferenceError.functionArg(myIndex), getSourceNode(), candidates);
  }

  @Override
  public TypeCheckingError getErrorMismatch(Expression expectedType, Type actualType, Expression candidate) {
    return new ArgInferenceError(ArgInferenceError.functionArg(myIndex), expectedType, actualType, getSourceNode(), candidate);
  }
}
