package org.arend.typechecking.error.local;

import org.arend.error.doc.LineDoc;
import org.arend.naming.reference.GlobalReferable;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import static org.arend.error.doc.DocFactory.*;

public class FieldsDependentImplementationError extends TypecheckingError {
  public GlobalReferable implementedField;
  public GlobalReferable referredField;

  public FieldsDependentImplementationError(GlobalReferable implementedField, GlobalReferable referredField, Concrete.SourceNode cause) {
    super("", cause);
    this.implementedField = implementedField;
    this.referredField = referredField;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig src) {
    return hList(text("Field "), refDoc(implementedField), text(" cannot be implemented since it depends on field "), refDoc(referredField), text(" which is not implemented"));
  }
}
