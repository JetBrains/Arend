package org.arend.typechecking.error.local;

import org.arend.core.expr.DataCallExpression;
import org.arend.error.doc.Doc;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import javax.annotation.Nonnull;

import static org.arend.error.doc.DocFactory.*;

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
  public boolean isShort() {
    return false;
  }
}
