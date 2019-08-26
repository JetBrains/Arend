package org.arend.typechecking.error.local.inference;

import org.arend.core.expr.Expression;
import org.arend.term.concrete.Concrete;

public class FunctionArgInferenceError extends ArgInferenceError {
  public final int index;

  public FunctionArgInferenceError(int index, Concrete.SourceNode cause, Expression[] candidates) {
    super(message(index), cause, candidates);
    this.index = index;
  }

  public FunctionArgInferenceError(int index, Expression expected, Expression actual, Concrete.SourceNode cause, Expression candidate) {
    super(message(index), expected, actual, cause, candidate);
    this.index = index;
  }

  private static String message(int index) {
    return "Cannot infer the " + ordinal(index) + " argument";
  }
}
