package org.arend.ext.prettyprinting;

import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.DocFactory;

public interface PrettyPrintable {
  void prettyPrint(StringBuilder builder, PrettyPrinterConfig ppConfig);

  default Doc prettyPrint(PrettyPrinterConfig ppConfig) {
    return DocFactory.ppDoc(this, ppConfig);
  }
}
