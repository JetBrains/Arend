package org.arend.term.prettyprint;

import org.arend.error.doc.Doc;
import org.arend.error.doc.DocFactory;

public interface PrettyPrintable {
  void prettyPrint(StringBuilder builder, PrettyPrinterConfig ppConfig);

  default Doc prettyPrint(PrettyPrinterConfig ppConfig) {
    return DocFactory.ppDoc(this, ppConfig);
  }
}
