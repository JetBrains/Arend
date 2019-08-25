package org.arend.error.doc;

import org.arend.term.prettyprint.PrettyPrintable;
import org.arend.term.prettyprint.PrettyPrinterConfig;

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
  public String getString() {
    StringBuilder builder = new StringBuilder();
    myPrettyPrintable.prettyPrint(builder, myPPConfig);
    return builder.toString();
  }
}
