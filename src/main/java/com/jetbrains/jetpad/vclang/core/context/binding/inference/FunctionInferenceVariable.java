package com.jetbrains.jetpad.vclang.core.context.binding.inference;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

import java.util.Set;

public class FunctionInferenceVariable<T> extends InferenceVariable<T> {
  private final int myIndex;
  private final Definition myDefinition;

  public FunctionInferenceVariable(String name, Expression type, int index, Definition definition, Concrete.SourceNode<T> sourceNode, Set<Binding> bounds) {
    super(name, type, sourceNode, bounds);
    myIndex = index;
    myDefinition = definition;
  }

  @Override
  public LocalTypeCheckingError<T> getErrorInfer(Expression... candidates) {
    return new ArgInferenceError<>(ArgInferenceError.functionArg(myIndex, myDefinition != null ? myDefinition.getName() : null), getSourceNode(), candidates);
  }

  @Override
  public LocalTypeCheckingError<T> getErrorMismatch(Expression expectedType, Expression actualType, Expression candidate) {
    return new ArgInferenceError<>(ArgInferenceError.functionArg(myIndex, myDefinition != null ? myDefinition.getName() : null), expectedType, actualType, getSourceNode(), candidate);
  }
}
