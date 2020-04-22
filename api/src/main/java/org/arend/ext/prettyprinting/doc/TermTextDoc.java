package org.arend.ext.prettyprinting.doc;

import org.arend.ext.core.expr.CoreExpression;

public class TermTextDoc extends TextDoc {
  private final CoreExpression term;
  private final boolean isFirst;

  TermTextDoc(String text, CoreExpression term, boolean isFirst) {
    super(text);
    this.term = term;
    this.isFirst = isFirst;
  }

  public CoreExpression getTerm() {
    return term;
  }

  public boolean isFirst() {
    return isFirst;
  }
}
