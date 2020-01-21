package org.arend.typechecking.error.local;

import org.arend.core.expr.DataCallExpression;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.term.concrete.Concrete;

import javax.annotation.Nonnull;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class ImpossibleEliminationError extends TypecheckingError {
  public final DataCallExpression dataCall;

  public ImpossibleEliminationError(DataCallExpression dataCall, @Nonnull Concrete.SourceNode cause) {
    super("Elimination is not possible here, cannot determine the set of eligible constructors", cause);
    this.dataCall = dataCall;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return hList(text("for data "), termLine(dataCall, ppConfig));
  }

  @Override
  public boolean hasExpressions() {
    return true;
  }
}
