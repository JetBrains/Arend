package com.jetbrains.jetpad.vclang.term.context.binding.inference;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError;

public class ExpressionInferenceBinding extends InferenceBinding {
  private final Abstract.SourceNode mySourceNode;

  public ExpressionInferenceBinding(Expression type, Abstract.SourceNode sourceNode) {
    super(null, type);
    mySourceNode = sourceNode;
  }

  @Override
  public Abstract.SourceNode getSourceNode() {
    return mySourceNode;
  }

  @Override
  public void reportErrorInfer(ErrorReporter errorReporter, Expression... candidates) {
    errorReporter.report(new ArgInferenceError(ArgInferenceError.expression(), mySourceNode, candidates));
  }

  @Override
  public void reportErrorLevelInfer(ErrorReporter errorReporter, Level... candidates) {
    throw new IllegalStateException();
    //errorReporter.report(new ArgInferenceError(ArgInferenceError.expression(), mySourceNode, new Expression[0], candidates));
  }

  @Override
  public void reportErrorMismatch(ErrorReporter errorReporter, Expression expectedType, Type actualType, Expression candidate) {
    errorReporter.report(new ArgInferenceError(ArgInferenceError.expression(), expectedType, actualType, mySourceNode, candidate));
  }
}
