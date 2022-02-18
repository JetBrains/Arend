package org.arend.core.context.binding.inference;

import org.arend.core.context.binding.Binding;
import org.arend.core.expr.Expression;
import org.arend.term.concrete.Concrete;
import org.arend.ext.error.LocalError;
import org.arend.typechecking.error.local.inference.ArgInferenceError;

import java.util.Set;

public class ExpressionInferenceVariable extends InferenceVariable {
  private final boolean mySolvableFromEquations;
  private final boolean myUseSubstExpr;

  public ExpressionInferenceVariable(Expression type, Concrete.SourceNode sourceNode, Set<Binding> bounds, boolean solvableFromEquations, boolean useSubstExpr) {
    super("H", type, sourceNode, bounds);
    mySolvableFromEquations = solvableFromEquations;
    myUseSubstExpr = useSubstExpr;
  }

  public ExpressionInferenceVariable(Expression type, Concrete.SourceNode sourceNode, Set<Binding> bounds, boolean solvableFromEquations) {
    this(type, sourceNode, bounds, solvableFromEquations, false);
  }

  @Override
  public boolean useSubstExpr() {
    return myUseSubstExpr;
  }

  @Override
  public boolean isSolvableFromEquations() {
    return mySolvableFromEquations;
  }

  @Override
  public boolean resetClassCall() {
    return false;
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
