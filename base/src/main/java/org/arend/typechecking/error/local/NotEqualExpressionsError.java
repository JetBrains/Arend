package org.arend.typechecking.error.local;

import org.arend.core.expr.Expression;
import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.jetbrains.annotations.Nullable;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class NotEqualExpressionsError extends TypecheckingError {
  public final Expression expr1;
  public final Expression expr2;

  public NotEqualExpressionsError(Expression expr1, Expression expr2, @Nullable ConcreteSourceNode cause) {
    super(Level.ERROR, "Expressions are not equal", cause);
    this.expr1 = expr1;
    this.expr2 = expr2;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return vList(
      hang(text("Left: "), termDoc(expr1, ppConfig)),
      hang(text("Right:"), termDoc(expr2, ppConfig)));
  }

  @Override
  public boolean hasExpressions() {
    return true;
  }
}
