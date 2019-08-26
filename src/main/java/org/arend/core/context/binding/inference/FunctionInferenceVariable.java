package org.arend.core.context.binding.inference;

import org.arend.core.context.binding.Binding;
import org.arend.core.expr.Expression;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.LocalError;
import org.arend.typechecking.error.local.inference.FunctionArgInferenceError;

import java.util.Set;

public class FunctionInferenceVariable extends InferenceVariable {
  private final int myIndex;

  public FunctionInferenceVariable(String name, Expression type, int index, Concrete.SourceNode sourceNode, Set<Binding> bounds) {
    super(name, type, sourceNode, bounds);
    myIndex = index;
  }

  @Override
  public LocalError getErrorInfer(Expression... candidates) {
    return new FunctionArgInferenceError(myIndex, getSourceNode(), candidates);
  }

  @Override
  public LocalError getErrorMismatch(Expression expectedType, Expression actualType, Expression candidate) {
    return new FunctionArgInferenceError(myIndex, expectedType, actualType, getSourceNode(), candidate);
  }
}
