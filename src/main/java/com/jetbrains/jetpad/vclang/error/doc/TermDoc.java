package com.jetbrains.jetpad.vclang.error.doc;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

public class TermDoc extends CachingDoc {
  private final Expression myTerm;
  private final PrettyPrinterConfig myPPConfig;

  TermDoc(Expression term, PrettyPrinterConfig ppConfig) {
    myTerm = term;
    myPPConfig = ppConfig;
  }

  public Expression getTerm() {
    return myTerm;
  }

  @Override
  protected String getString() {
    StringBuilder builder = new StringBuilder();
    myTerm.prettyPrint(builder, myPPConfig);
    return builder.toString();
  }

  @Override
  public boolean isEmpty() {
    return false;
  }
}
