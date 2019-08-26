package org.arend.typechecking.error.local.inference;

import org.arend.core.expr.Expression;
import org.arend.error.doc.LineDoc;
import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import static org.arend.error.doc.DocFactory.*;

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
    return isLevel
      ? hList(text("Cannot infer level of the type of parameter '"), refDoc(parameter), text("'"))
      : hList(text("Cannot infer type of parameter '"), refDoc(parameter), text("'"));
  }
}
