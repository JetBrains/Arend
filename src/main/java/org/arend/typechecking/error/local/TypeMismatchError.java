package org.arend.typechecking.error.local;

import org.arend.error.doc.Doc;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrintable;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import static org.arend.error.doc.DocFactory.*;

public class TypeMismatchError extends TypecheckingError {
  public final PrettyPrintable expected;
  public final PrettyPrintable actual;

  public TypeMismatchError(PrettyPrintable expected, PrettyPrintable actual, Concrete.SourceNode sourceNode) {
    super("Type mismatch", sourceNode);
    this.expected = expected;
    this.actual = actual;
  }

  public TypeMismatchError(String msg, PrettyPrintable expected, PrettyPrintable actual, Concrete.SourceNode sourceNode) {
    super(msg, sourceNode);
    this.expected = expected;
    this.actual = actual;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    Doc expectedDoc = hang(text("Expected type:"), expected.prettyPrint(ppConfig));
    return vList(
      expectedDoc,
      hang(text(expectedDoc.getHeight() == 1 ? "  Actual type:" : "Actual type:"), actual.prettyPrint(ppConfig)));
  }
}
