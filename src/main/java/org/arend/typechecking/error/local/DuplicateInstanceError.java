package org.arend.typechecking.error.local;

import org.arend.core.expr.Expression;
import org.arend.error.doc.Doc;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import static org.arend.error.doc.DocFactory.*;

public class DuplicateInstanceError extends TypecheckingError {
  public final Expression oldInstance;
  public final Expression newInstance;

  public DuplicateInstanceError(Expression oldInstance, Expression newInstance, Concrete.SourceNode cause) {
    super(Level.WARNING, "Duplicate instance", cause);
    this.oldInstance = oldInstance;
    this.newInstance = newInstance;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return vList(
      hang(text("Old instance:"), termDoc(oldInstance, ppConfig)),
      hang(text("New instance:"), termDoc(newInstance, ppConfig)));
  }
}
