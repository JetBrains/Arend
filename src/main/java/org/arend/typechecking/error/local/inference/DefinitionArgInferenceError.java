package org.arend.typechecking.error.local.inference;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.Definition;
import org.arend.core.expr.Expression;
import org.arend.term.concrete.Concrete;

public class DefinitionArgInferenceError extends ArgInferenceError {
  public final Definition definition;
  public final DependentLink parameter;

  public DefinitionArgInferenceError(Definition definition, DependentLink parameter, Concrete.SourceNode cause, Expression[] candidates) {
    super(message(definition, parameter), cause, candidates);
    this.definition = definition;
    this.parameter = parameter;
  }

  public DefinitionArgInferenceError(Definition definition, DependentLink parameter, Expression expected, Expression actual, Concrete.SourceNode cause, Expression candidate) {
    super(message(definition, parameter), expected, actual, cause, candidate);
    this.definition = definition;
    this.parameter = parameter;
  }

  private static String message(Definition definition, DependentLink parameter) {
    return "Cannot infer parameter '" + parameter.getName() + "' of definition '" + definition.getName() + "'";
  }
}
