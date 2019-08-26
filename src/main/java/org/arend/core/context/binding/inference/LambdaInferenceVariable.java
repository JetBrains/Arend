package org.arend.core.context.binding.inference;

import org.arend.core.context.binding.Binding;
import org.arend.core.expr.Expression;
import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.LocalError;
import org.arend.typechecking.error.local.inference.LambdaInferenceError;

import java.util.Set;

public class LambdaInferenceVariable extends InferenceVariable {
  private final Referable myParameter;
  private final boolean myLevel;

  public LambdaInferenceVariable(String name, Expression type, Referable parameter, boolean level, Concrete.SourceNode sourceNode, Set<Binding> bounds) {
    super(name, type, sourceNode, bounds);
    myParameter = parameter;
    myLevel = level;
  }

  @Override
  public LocalError getErrorInfer(Expression... candidates) {
    return new LambdaInferenceError(myParameter, myLevel, getSourceNode(), candidates);
  }

  @Override
  public LocalError getErrorMismatch(Expression expectedType, Expression actualType, Expression candidate) {
    return new LambdaInferenceError(myParameter, myLevel, expectedType, actualType, getSourceNode(), candidate);
  }
}
