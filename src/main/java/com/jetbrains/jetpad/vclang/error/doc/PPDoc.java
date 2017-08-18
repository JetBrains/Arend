package com.jetbrains.jetpad.vclang.error.doc;

import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

public class PPDoc extends CachingDoc {
  private final PrettyPrintable myPrettyPrintable;
  private final PrettyPrinterInfoProvider myInfoProvider;

  PPDoc(PrettyPrintable prettyPrintable, PrettyPrinterInfoProvider infoProvider) {
    myPrettyPrintable = prettyPrintable;
    myInfoProvider = infoProvider;
  }

  public PrettyPrintable getPrettyPrintable() {
    return myPrettyPrintable;
  }

  @Override
  protected String getString() {
    String text = myPrettyPrintable.prettyPrint(myInfoProvider);
    return text == null ? myPrettyPrintable.toString() : text;
  }
}
