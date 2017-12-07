package com.jetbrains.jetpad.vclang.error.doc;

import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

public class PPDoc extends CachingDoc {
  private final PrettyPrintable myPrettyPrintable;
  private final PrettyPrinterConfig myPPConfig;

  PPDoc(PrettyPrintable prettyPrintable, PrettyPrinterConfig ppConfig) {
    myPrettyPrintable = prettyPrintable;
    myPPConfig = ppConfig;
  }

  public PrettyPrintable getPrettyPrintable() {
    return myPrettyPrintable;
  }

  @Override
  protected String getString() {
    StringBuilder builder = new StringBuilder();
    myPrettyPrintable.prettyPrint(builder, myPPConfig);
    return builder.toString();
  }
}
