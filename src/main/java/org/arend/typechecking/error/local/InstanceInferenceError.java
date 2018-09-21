package org.arend.typechecking.error.local;

import org.arend.core.expr.Expression;
import org.arend.naming.reference.TCClassReferable;
import org.arend.term.concrete.Concrete;

public class InstanceInferenceError extends ArgInferenceError {
  public final TCClassReferable classRef;

  private InstanceInferenceError(TCClassReferable classRef, Expression expected, Expression actual, Concrete.SourceNode cause, Expression[] candidates) {
    super("Cannot infer an instance of class '" + classRef.textRepresentation() + "'", expected, actual, cause, candidates);
    this.classRef = classRef;
  }

  public InstanceInferenceError(TCClassReferable classRef, Concrete.SourceNode cause, Expression[] candidates) {
    this(classRef, null, null, cause, candidates);
  }

  public InstanceInferenceError(TCClassReferable classRef, Expression expected, Expression actual, Concrete.SourceNode cause, Expression candidate) {
    this(classRef, expected, actual, cause, new Expression[1]);
    candidates[0] = candidate;
  }
}
