package org.arend.typechecking.error.local;

import org.arend.core.expr.Expression;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.term.concrete.Concrete;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

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
      hang(text("2nd expression:"), termDoc(expr2, ppConfig)));
  }

  @Override
  public boolean hasExpressions() {
    return true;
  }
}
