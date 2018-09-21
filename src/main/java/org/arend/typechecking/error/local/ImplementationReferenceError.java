package org.arend.typechecking.error.local;

import org.arend.core.definition.ClassField;
import org.arend.error.doc.LineDoc;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import static org.arend.error.doc.DocFactory.*;

public class ImplementationReferenceError extends TypecheckingError {
  public ClassField classField;

  public ImplementationReferenceError(ClassField classField, Concrete.SourceNode cause) {
    super("The implementation refers to a non-implemented field ", cause);
    this.classField = classField;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig src) {
    return hList(text(message), refDoc(classField.getReferable()));
  }
}
