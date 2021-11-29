package org.arend.core.context.binding.inference;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Definition;
import org.arend.core.expr.Expression;
import org.arend.term.concrete.Concrete;
import org.arend.ext.error.LocalError;
import org.arend.typechecking.error.local.inference.FunctionArgInferenceError;

import java.util.Set;

public class FunctionInferenceVariable extends InferenceVariable {
  private final Definition myDefinition;
  private final DependentLink myParameter;
  private final int myIndex;

  public FunctionInferenceVariable(Definition definition, DependentLink parameter, int index, Expression type, Concrete.SourceNode sourceNode, Set<Binding> bounds) {
    super(parameter.getName(), type, sourceNode, bounds);
    myDefinition = definition;
    myParameter = parameter;
    myIndex = index;
  }

  @Override
  public boolean compareClassCallsExactly() {
    return myDefinition instanceof ClassField;
  }

  @Override
  public LocalError getErrorInfer(Expression... candidates) {
    return new FunctionArgInferenceError(myDefinition, myParameter, myIndex, getSourceNode(), candidates);
  }

  @Override
  public LocalError getErrorMismatch(Expression expectedType, Expression actualType, Expression candidate) {
    return new FunctionArgInferenceError(myDefinition, myParameter, myIndex, expectedType, actualType, getSourceNode(), candidate);
  }
}
