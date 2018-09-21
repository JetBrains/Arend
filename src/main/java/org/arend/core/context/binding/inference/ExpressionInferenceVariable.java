package org.arend.core.context.binding.inference;

import org.arend.core.context.binding.Binding;
import org.arend.core.expr.Expression;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.ArgInferenceError;
import org.arend.typechecking.error.local.LocalError;

import java.util.Set;

public class ExpressionInferenceVariable extends InferenceVariable {
  public ExpressionInferenceVariable(Expression type, Concrete.SourceNode sourceNode, Set<Binding> bounds) {
    super("H", type, sourceNode, bounds);
  }

  @Override
  public LocalError getErrorInfer(Expression... candidates) {
    return new ArgInferenceError(ArgInferenceError.expression(), getSourceNode(), candidates);
  }

  @Override
  public LocalError getErrorMismatch(Expression expectedType, Expression actualType, Expression candidate) {
    return new ArgInferenceError(ArgInferenceError.expression(), expectedType, actualType, getSourceNode(), candidate);
  }
}
