package com.jetbrains.jetpad.vclang.error.doc;

public class HangDoc extends Doc {
  private final Doc myTop;
  private final Doc myBottom;

  public final static int INDENT = 2;
  public final static int MAX_INDENT = 6;

  HangDoc(Doc top, Doc bottom) {
    myTop = top;
    myBottom = bottom;
  }

  public Doc getTop() {
    return myTop;
  }

  public Doc getBottom() {
    return myBottom;
  }

  @Override
  public <P, R> R accept(DocVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitHang(this, params);
  }

  @Override
  public int getWidth() {
    if (myBottom.isNull()) {
      return myTop.getWidth();
    }
    if (myTop.isEmpty()) {
      return myBottom.getWidth() + INDENT;
    }

    if (needNewLine()) {
      return Math.max(myTop.getWidth(), myBottom.getWidth() + INDENT);
    } else {
      int bottomWidth = myBottom.getWidth();
      return myTop.getWidth() + (bottomWidth == 0 ? 0 : 1) + bottomWidth;
    }
  }

  @Override
  public int getHeight() {
    return myBottom.isNull() ? myTop.getHeight() : needNewLine() ? myTop.getHeight() + myBottom.getHeight() : myBottom.getHeight();
  }

  @Override
  public boolean isNull() {
    return myTop.isNull() && myBottom.isNull();
  }

  @Override
  public boolean isSingleLine() {
    return myTop.isSingleLine() && myBottom.isSingleLine();
  }

  @Override
  public boolean isEmpty() {
    return myBottom.isNull() && myTop.isEmpty();
  }

  public boolean needNewLine() {
    return !myTop.isSingleLine() && !myBottom.isNull() || !myBottom.isSingleLine() && myTop.getWidth() + (myBottom.isEmpty() ? 0 : 1) > HangDoc.MAX_INDENT;
  }
}
