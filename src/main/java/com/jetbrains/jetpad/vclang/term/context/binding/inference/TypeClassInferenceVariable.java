package com.jetbrains.jetpad.vclang.term.context.binding.inference;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.term.typeclass.ClassView;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

public class TypeClassInferenceVariable extends InferenceVariable {
  private final ClassView myClassView;

  public TypeClassInferenceVariable(String name, Expression type, ClassView classView, Abstract.SourceNode sourceNode) {
    super(name, type, sourceNode);
    myClassView = classView;
  }

  public ClassView getClassView() {
    return myClassView;
  }

  @Override
  public LocalTypeCheckingError getErrorInfer(Expression... candidates) {
    return new ArgInferenceError(ArgInferenceError.typeClass(), getSourceNode(), candidates);
  }

  @Override
  public LocalTypeCheckingError getErrorMismatch(Expression expectedType, Type actualType, Expression candidate) {
    return new ArgInferenceError(ArgInferenceError.typeClass(), expectedType, actualType, getSourceNode(), candidate);
  }
}
