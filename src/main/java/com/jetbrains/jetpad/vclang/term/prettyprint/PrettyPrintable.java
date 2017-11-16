package com.jetbrains.jetpad.vclang.term.prettyprint;

import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.DocFactory;

public interface PrettyPrintable {
  void prettyPrint(StringBuilder builder, PrettyPrinterConfig ppConfig);

  default Doc prettyPrint(PrettyPrinterConfig ppConfig) {
    StringBuilder builder = new StringBuilder();
    prettyPrint(builder, ppConfig);
    return DocFactory.text(builder.toString());
  }
}
