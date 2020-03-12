package org.arend.ext.prettyprinting.doc;

public class TermTextDoc extends TextDoc {
  private final boolean isFirst;

  TermTextDoc(String text, boolean isFirst) {
    super(text);
    this.isFirst = isFirst;
  }

  public boolean isFirst() {
    return isFirst;
  }
}
