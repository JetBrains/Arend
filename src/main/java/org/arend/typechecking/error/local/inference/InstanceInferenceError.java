package org.arend.typechecking.error.local.inference;

import org.arend.core.expr.Expression;
import org.arend.error.doc.Doc;
import org.arend.naming.reference.TCClassReferable;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import static org.arend.error.doc.DocFactory.*;

public class InstanceInferenceError extends ArgInferenceError {
  public final TCClassReferable classRef;
  public final Expression classifyingExpression;

  private InstanceInferenceError(TCClassReferable classRef, Expression expected, Expression actual, Expression classifyingExpression, Concrete.SourceNode cause, Expression[] candidates) {
    super("Cannot infer an instance of class '" + classRef.textRepresentation() + "'", expected, actual, cause, candidates);
    this.classRef = classRef;
    this.classifyingExpression = classifyingExpression;
  }

  public InstanceInferenceError(TCClassReferable classRef, Concrete.SourceNode cause, Expression[] candidates) {
    this(classRef, null, null, null, cause, candidates);
  }

  public InstanceInferenceError(TCClassReferable classRef, Expression classifyingExpression, Concrete.SourceNode cause) {
    this(classRef, null, null, classifyingExpression, cause, new Expression[0]);
  }

  public InstanceInferenceError(TCClassReferable classRef, Expression expected, Expression actual, Concrete.SourceNode cause, Expression candidate) {
    this(classRef, expected, actual, null, cause, new Expression[1]);
    candidates[0] = candidate;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return classifyingExpression == null ? super.getBodyDoc(ppConfig) : hang(text("Classifying expression:"), termDoc(classifyingExpression, ppConfig));
  }
}
