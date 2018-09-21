package org.arend.core.context.binding.inference;

import org.arend.core.context.binding.Binding;
import org.arend.core.expr.Expression;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.ArgInferenceError;
import org.arend.typechecking.error.local.LocalError;

import java.util.Set;

public class LambdaInferenceVariable extends InferenceVariable {
  private final int myIndex;
  private final boolean myLevel;

  public LambdaInferenceVariable(String name, Expression type, int index, Concrete.SourceNode sourceNode, boolean level, Set<Binding> bounds) {
    super(name, type, sourceNode, bounds);
    myIndex = index;
    myLevel = level;
  }

  @Override
  public LocalError getErrorInfer(Expression... candidates) {
    return new ArgInferenceError(myLevel ? ArgInferenceError.levelOfLambdaArg(myIndex) : ArgInferenceError.lambdaArg(myIndex), getSourceNode(), candidates);
  }

  @Override
  public LocalError getErrorMismatch(Expression expectedType, Expression actualType, Expression candidate) {
    return new ArgInferenceError(myLevel ? ArgInferenceError.levelOfLambdaArg(myIndex) : ArgInferenceError.lambdaArg(myIndex), expectedType, actualType, getSourceNode(), candidate);
  }
}
