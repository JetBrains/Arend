package com.jetbrains.jetpad.vclang.term.context.binding.inference;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.typechecking.error.LevelInferenceError;

public class LevelInferenceBinding extends InferenceBinding {
  private final Abstract.SourceNode mySourceNode;

  public LevelInferenceBinding(String name, Expression type, Abstract.SourceNode sourceNode) {
    super(name, type);
    mySourceNode = sourceNode;
  }

  @Override
  public Abstract.SourceNode getSourceNode() {
    return mySourceNode;
  }

  @Override
  public void reportErrorInfer(ErrorReporter errorReporter, Expression... candidates) {
    errorReporter.report(new LevelInferenceError(this, mySourceNode));
  }

  @Override
  public void reportErrorLevelInfer(ErrorReporter errorReporter, Level... candidates) {
    errorReporter.report(new LevelInferenceError(this, mySourceNode, candidates));
  }

  @Override
  public void reportErrorMismatch(ErrorReporter errorReporter, Expression expectedType, Type actualType, Expression candidate) {
    errorReporter.report(new LevelInferenceError(this, mySourceNode));
  }
}
