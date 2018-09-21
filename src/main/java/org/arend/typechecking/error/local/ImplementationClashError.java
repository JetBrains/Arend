package org.arend.typechecking.error.local;

import org.arend.core.definition.ClassField;
import org.arend.core.expr.Expression;
import org.arend.error.doc.Doc;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import static org.arend.error.doc.DocFactory.*;

public class ImplementationClashError extends TypecheckingError {
  public final Expression oldImplementation;
  public final Expression newImplementation;

  public ImplementationClashError(Expression oldImplementation, ClassField classField, Expression newImplementation, Concrete.SourceNode cause) {
    super("New implementation of field '" + classField.getName() + "' differs from the previous one", cause);
    this.oldImplementation = oldImplementation;
    this.newImplementation = newImplementation;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return vList(
      hang(text("Old:"), oldImplementation.prettyPrint(ppConfig)),
      hang(text("New:"), newImplementation.prettyPrint(ppConfig)));
  }
}
