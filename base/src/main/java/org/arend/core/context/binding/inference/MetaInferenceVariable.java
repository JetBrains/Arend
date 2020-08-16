package org.arend.core.context.binding.inference;

import org.arend.core.context.binding.Binding;
import org.arend.core.expr.Expression;
import org.arend.ext.error.LocalError;
import org.arend.ext.error.TypeMismatchError;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.typechecking.MetaDefinition;
import org.arend.term.concrete.Concrete;

import java.util.Set;

public class MetaInferenceVariable extends InferenceVariable {
  public MetaInferenceVariable(Expression type, Concrete.ReferenceExpression reference, Set<Binding> bounds) {
    super(reference.getReferent().getRefName(), type, reference, bounds);
  }

  @Override
  public boolean isSolvableFromEquations() {
    return false;
  }

  @Override
  public LocalError getErrorInfer(Expression... candidates) {
    return new TypecheckingError("Deferred meta '" + getName() + "' was not invoked yet", getSourceNode());
  }

  @Override
  public LocalError getErrorMismatch(Expression expectedType, Expression actualType, Expression candidate) {
    return new TypeMismatchError(expectedType, actualType, getSourceNode());
  }
}
