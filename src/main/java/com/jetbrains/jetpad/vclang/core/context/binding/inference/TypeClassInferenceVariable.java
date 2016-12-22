package com.jetbrains.jetpad.vclang.core.context.binding.inference;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeMax;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

public class TypeClassInferenceVariable extends InferenceVariable {
  private final Abstract.ClassView myClassView;
  private final boolean myExactClassView;

  public TypeClassInferenceVariable(String name, Expression type, Abstract.ClassView classView, boolean isExactClassView, Abstract.SourceNode sourceNode) {
    super(name, type, sourceNode);
    myClassView = classView;
    myExactClassView = isExactClassView;
  }

  public Abstract.ClassView getClassView() {
    return myClassView;
  }

  public boolean isExactClassView() {
    return myExactClassView;
  }

  @Override
  public LocalTypeCheckingError getErrorInfer(Expression... candidates) {
    return new ArgInferenceError(ArgInferenceError.typeClass(), getSourceNode(), candidates);
  }

  @Override
  public LocalTypeCheckingError getErrorMismatch(Type expectedType, TypeMax actualType, Expression candidate) {
    return new ArgInferenceError(ArgInferenceError.typeClass(), expectedType, actualType, getSourceNode(), candidate);
  }
}
