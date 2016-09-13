package com.jetbrains.jetpad.vclang.term.context.binding.inference;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LevelInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

public class LevelInferenceVariable extends InferenceVariable {
  private final Abstract.SourceNode mySourceNode;

  public LevelInferenceVariable(String name, Expression type, Abstract.SourceNode sourceNode) {
    super(name, type);
    mySourceNode = sourceNode;
  }

  @Override
  public Abstract.SourceNode getSourceNode() {
    return mySourceNode;
  }

  @Override
  public LocalTypeCheckingError getErrorInfer(Expression... candidates) {
    return new LevelInferenceError(this, mySourceNode);
  }

  @Override
  public LocalTypeCheckingError getErrorMismatch(Expression expectedType, Type actualType, Expression candidate) {
    return new LevelInferenceError(this, mySourceNode);
  }
}
