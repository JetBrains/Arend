package org.arend.typechecking.error.local.inference;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.Definition;
import org.arend.core.expr.Expression;
import org.arend.ext.error.ArgInferenceError;
import org.arend.term.concrete.Concrete;

public class FunctionArgInferenceError extends ArgInferenceError {
  public final Definition definition;
  public final DependentLink parameter;
  public final int index;

  public FunctionArgInferenceError(Definition definition, DependentLink parameter, int index, Concrete.SourceNode cause, Expression[] candidates) {
    super(message(definition, parameter, index), cause, candidates);
    this.definition = definition;
    this.parameter = parameter;
    this.index = index;
  }

  public FunctionArgInferenceError(Definition definition, DependentLink parameter, int index, Expression expected, Expression actual, Concrete.SourceNode cause, Expression candidate) {
    super(message(definition, parameter, index), expected, actual, cause, candidate);
    this.definition = definition;
    this.parameter = parameter;
    this.index = index;
  }

  private static String message(Definition definition, DependentLink parameter, int index) {
    return parameter.getName() != null
      ? "Cannot infer parameter '" + parameter.getName() + "'" + message(definition)
      : "Cannot infer the " + ordinal(index) + " parameter" + message(definition);
  }

  private static String message(Definition definition) {
    return definition == null ? "" : " of definition '" + definition.getName() + "'";
  }
}
