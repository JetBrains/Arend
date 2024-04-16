package org.arend.typechecking.error.local;

import org.arend.core.expr.Expression;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettifier.ExpressionPrettifier;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.term.concrete.Concrete;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class PathEndpointMismatchError extends TypecheckingError {
  private final ExpressionPrettifier myPrettifier;
  public final boolean isLeft;
  public final Expression expected;
  public final Expression actual;

  public PathEndpointMismatchError(ExpressionPrettifier prettifier, boolean isLeft, Expression expected, Expression actual, Concrete.SourceNode cause) {
    super("The " + (isLeft ? "left" : "right") + " path endpoint mismatch", cause);
    myPrettifier = prettifier;
    this.isLeft = isLeft;
    this.expected = expected;
    this.actual = actual;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return vList(
      hang(text("Expected:"), termDoc(expected, myPrettifier, ppConfig)),
      hang(text("  Actual:"), termDoc(actual, myPrettifier, ppConfig)));
  }

  @Override
  public boolean hasExpressions() {
    return true;
  }
}
