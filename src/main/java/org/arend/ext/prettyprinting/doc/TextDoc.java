package org.arend.ext.prettyprinting.doc;

public class TextDoc extends LineDoc {
  private final String text;

  TextDoc(String text) {
    this.text = text;
  }

  public String getText() {
    return text;
  }

  @Override
  public <P, R> R accept(DocVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitText(this, params);
  }

  @Override
  public int getWidth() {
    return text.length();
  }

  @Override
  public boolean isEmpty() {
    return text.isEmpty();
  }
}
