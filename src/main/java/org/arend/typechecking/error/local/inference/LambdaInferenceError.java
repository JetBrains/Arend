package org.arend.typechecking.error.local.inference;

import org.arend.core.expr.Expression;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class LambdaInferenceError extends ArgInferenceError {
  public final Referable parameter;
  public final boolean isLevel;

  public LambdaInferenceError(Referable parameter, boolean isLevel, Concrete.SourceNode cause, Expression[] candidates) {
    super("", cause, candidates);
    this.parameter = parameter;
    this.isLevel = isLevel;
  }

  public LambdaInferenceError(Referable parameter, boolean isLevel, Expression expected, Expression actual, Concrete.SourceNode cause, Expression candidate) {
    super("", expected, actual, cause, candidate);
    this.parameter = parameter;
    this.isLevel = isLevel;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    String start = isLevel ? "Cannot infer level of the type of " : "Cannot infer type of ";
    return parameter == null
      ? text(start + "a lambda parameter")
      : hList(text(start + "parameter '"), refDoc(parameter), text("'"));
  }
}
