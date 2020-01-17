package org.arend.typechecking.error.local;

import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class IncorrectReferenceError extends TypecheckingError {
  public final Referable referable;

  public IncorrectReferenceError(Referable referable, Concrete.SourceNode sourceNode) {
    super("", sourceNode);
    this.referable = referable;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig src) {
    return hList(text("'"), refDoc(referable), text("' is not a reference to either a definition or a variable"));
  }
}
