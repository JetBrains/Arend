package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.naming.reference.TCClassReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;

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
