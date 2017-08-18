package com.jetbrains.jetpad.vclang.core.context.binding.inference;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.pool.ClassViewInstancePool;

import java.util.Set;

public class TypeClassInferenceVariable extends InferenceVariable {
  private final Abstract.ClassView myClassView;
  private final boolean isView;

  public TypeClassInferenceVariable(String name, Expression type, Abstract.ClassView classView, boolean isView, Abstract.SourceNode sourceNode, Set<Binding> bounds) {
    super(name, type, sourceNode, bounds);
    myClassView = classView;
    this.isView = isView;
  }

  public Abstract.ClassView getClassView() {
    return myClassView;
  }

  @Override
  public LocalTypeCheckingError getErrorInfer(Expression... candidates) {
    return new ArgInferenceError(ArgInferenceError.typeClass(), (Concrete.SourceNode) getSourceNode(), candidates);
  }

  @Override
  public LocalTypeCheckingError getErrorMismatch(Expression expectedType, Expression actualType, Expression candidate) {
    return new ArgInferenceError(ArgInferenceError.typeClass(), expectedType, actualType, (Concrete.SourceNode) getSourceNode(), candidate);
  }

  public Expression getInstance(ClassViewInstancePool pool, Expression classifyingExpression) {
    return pool.getInstance(classifyingExpression, myClassView, isView);
  }
}
