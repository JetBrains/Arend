package org.arend.core.context.binding.inference;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.Definition;
import org.arend.core.expr.Expression;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.LocalError;
import org.arend.typechecking.error.local.inference.DefinitionArgInferenceError;

import java.util.Set;

public class DefinitionInferenceVariable extends InferenceVariable {
  private final Definition myDefinition;
  private final DependentLink myParameter;

  public DefinitionInferenceVariable(Definition definition, DependentLink parameter, Expression type, Concrete.SourceNode sourceNode, Set<Binding> bounds) {
    super(parameter.getName(), type, sourceNode, bounds);
    myDefinition = definition;
    myParameter = parameter;
  }

  @Override
  public LocalError getErrorInfer(Expression... candidates) {
    return new DefinitionArgInferenceError(myDefinition, myParameter, getSourceNode(), candidates);
  }

  @Override
  public LocalError getErrorMismatch(Expression expectedType, Expression actualType, Expression candidate) {
    return new DefinitionArgInferenceError(myDefinition, myParameter, expectedType, actualType, getSourceNode(), candidate);
  }
}
