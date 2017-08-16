package com.jetbrains.jetpad.vclang.core.context.binding.inference;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

import java.util.Set;

public class FunctionInferenceVariable extends InferenceVariable {
  private final int myIndex;
  private final Definition myDefinition;

  public FunctionInferenceVariable(String name, Expression type, int index, Definition definition, Abstract.SourceNode sourceNode, Set<Binding> bounds) {
    super(name, type, sourceNode, bounds);
    myIndex = index;
    myDefinition = definition;
  }

  @Override
  public LocalTypeCheckingError getErrorInfer(Expression... candidates) {
    return new ArgInferenceError(ArgInferenceError.functionArg(myIndex, myDefinition != null ? myDefinition.getName() : null), getSourceNode(), candidates);
  }

  @Override
  public LocalTypeCheckingError getErrorMismatch(Expression expectedType, Expression actualType, Expression candidate) {
    return new ArgInferenceError(ArgInferenceError.functionArg(myIndex, myDefinition != null ? myDefinition.getName() : null), expectedType, actualType, getSourceNode(), candidate);
  }
}
