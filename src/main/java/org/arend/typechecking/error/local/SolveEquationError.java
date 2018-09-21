package org.arend.typechecking.error.local;

import org.arend.core.expr.Expression;
import org.arend.error.doc.Doc;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import static org.arend.error.doc.DocFactory.*;

public class SolveEquationError extends TypecheckingError {
  public final Expression expr1;
  public final Expression expr2;

  public SolveEquationError(Expression expr1, Expression expr2, Concrete.SourceNode expression) {
    super("Cannot solve equation", expression);
    this.expr1 = expr1;
    this.expr2 = expr2;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return vList(
      hang(text("1st expression:"), termDoc(expr1, ppConfig)),
      hang(text("2st expression:"), termDoc(expr2, ppConfig)));
  }
}
