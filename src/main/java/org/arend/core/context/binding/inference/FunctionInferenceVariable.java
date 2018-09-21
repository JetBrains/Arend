package org.arend.core.context.binding.inference;

import org.arend.core.context.binding.Binding;
import org.arend.core.definition.Definition;
import org.arend.core.expr.Expression;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.ArgInferenceError;
import org.arend.typechecking.error.local.LocalError;

import java.util.Set;

public class FunctionInferenceVariable extends InferenceVariable {
  private final int myIndex;
  private final Definition myDefinition;

  public FunctionInferenceVariable(String name, Expression type, int index, Definition definition, Concrete.SourceNode sourceNode, Set<Binding> bounds) {
    super(name, type, sourceNode, bounds);
    myIndex = index;
    myDefinition = definition;
  }

  @Override
  public LocalError getErrorInfer(Expression... candidates) {
    return new ArgInferenceError(ArgInferenceError.functionArg(myIndex, myDefinition != null ? myDefinition.getName() : null), getSourceNode(), candidates);
  }

  @Override
  public LocalError getErrorMismatch(Expression expectedType, Expression actualType, Expression candidate) {
    return new ArgInferenceError(ArgInferenceError.functionArg(myIndex, myDefinition != null ? myDefinition.getName() : null), expectedType, actualType, getSourceNode(), candidate);
  }
}
