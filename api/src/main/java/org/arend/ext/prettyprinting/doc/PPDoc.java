package org.arend.ext.prettyprinting.doc;

import org.arend.ext.prettyprinting.PrettyPrintable;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;

public class PPDoc extends CachingDoc {
  private final PrettyPrintable prettyPrintable;
  private final PrettyPrinterConfig ppConfig;

  PPDoc(PrettyPrintable prettyPrintable, PrettyPrinterConfig ppConfig) {
    this.prettyPrintable = prettyPrintable;
    this.ppConfig = ppConfig;
  }

  public PrettyPrintable getPrettyPrintable() {
    return prettyPrintable;
  }

  @Override
  public String getString() {
    StringBuilder builder = new StringBuilder();
    prettyPrintable.prettyPrint(builder, ppConfig);
    return builder.toString();
  }
}
