package com.jetbrains.jetpad.vclang.term.context.binding.inference;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;

public class DerivedInferenceVariable extends InferenceVariable {
  private final InferenceVariable myBinding;

  public DerivedInferenceVariable(String name, InferenceVariable binding) {
    super(name, binding.getType());
    myBinding = binding;
  }

  @Override
  public Abstract.SourceNode getSourceNode() {
    return myBinding.getSourceNode();
  }

  @Override
  public void reportErrorInfer(ErrorReporter errorReporter, Expression... candidates) {
    myBinding.reportErrorInfer(errorReporter, candidates);
  }

  @Override
  public void reportErrorLevelInfer(ErrorReporter errorReporter, Level... candidates) {
    myBinding.reportErrorLevelInfer(errorReporter, candidates);
  }

  @Override
  public void reportErrorMismatch(ErrorReporter errorReporter, Expression expectedType, Type actualType, Expression candidate) {
    myBinding.reportErrorMismatch(errorReporter, expectedType, actualType, candidate);
  }
}
