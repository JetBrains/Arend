package org.arend.ext.error;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.prettyprinting.PrettyPrintable;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.DocFactory;

public class TypeMismatchError extends TypecheckingError {
  public final PrettyPrintable expected;
  public final PrettyPrintable actual;

  public TypeMismatchError(PrettyPrintable expected, PrettyPrintable actual, ConcreteSourceNode sourceNode) {
    super("Type mismatch", sourceNode);
    this.expected = expected;
    this.actual = actual;
  }

  public TypeMismatchError(String msg, PrettyPrintable expected, PrettyPrintable actual, ConcreteSourceNode sourceNode) {
    super(msg, sourceNode);
    this.expected = expected;
    this.actual = actual;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    Doc expectedDoc = DocFactory.hang(DocFactory.text("Expected type:"), expected.prettyPrint(ppConfig));
    return DocFactory.vList(
      expectedDoc,
      DocFactory.hang(DocFactory.text(expectedDoc.getHeight() == 1 ? "  Actual type:" : "Actual type:"), actual.prettyPrint(ppConfig)));
  }

  @Override
  public boolean hasExpressions() {
    return true;
  }
}
