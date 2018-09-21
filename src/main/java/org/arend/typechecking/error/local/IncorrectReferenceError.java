package org.arend.typechecking.error.local;

import org.arend.error.doc.LineDoc;
import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import static org.arend.error.doc.DocFactory.*;

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
