package org.arend.ext.prettyprinting.doc;

import org.arend.ext.core.expr.UncheckedExpression;

public class TermTextDoc extends TextDoc {
  private final UncheckedExpression term;
  private final boolean isFirst;

  TermTextDoc(String text, UncheckedExpression term, boolean isFirst) {
    super(text);
    this.term = term;
    this.isFirst = isFirst;
  }

  public UncheckedExpression getTerm() {
    return term;
  }

  public boolean isFirst() {
    return isFirst;
  }
}
