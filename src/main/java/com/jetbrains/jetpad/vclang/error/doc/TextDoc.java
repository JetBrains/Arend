package com.jetbrains.jetpad.vclang.error.doc;

public class TextDoc extends LineDoc {
  private final String myText;

  TextDoc(String text) {
    myText = text;
  }

  public String getText() {
    return myText;
  }

  @Override
  public <P, R> R accept(DocVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitText(this, params);
  }

  @Override
  public int getWidth() {
    return myText.length();
  }

  @Override
  public boolean isEmpty() {
    return myText.isEmpty();
  }
}
