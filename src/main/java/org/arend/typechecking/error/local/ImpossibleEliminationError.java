package org.arend.typechecking.error.local;

import org.arend.core.expr.DataCallExpression;
import org.arend.term.concrete.Concrete;

import javax.annotation.Nonnull;

public class ImpossibleEliminationError extends TypecheckingError {
  public final DataCallExpression dataCall;

  public ImpossibleEliminationError(DataCallExpression dataCall, @Nonnull Concrete.SourceNode cause) {
    super("Elimination is not possible here, cannot determine the set of eligible constructors", cause);
    this.dataCall = dataCall;
  }
}
