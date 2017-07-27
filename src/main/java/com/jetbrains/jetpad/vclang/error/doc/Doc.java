package com.jetbrains.jetpad.vclang.error.doc;

public abstract class Doc {
  public abstract <P, R> R accept(DocVisitor<? super P, ? extends R> visitor, P params);

  public abstract int getWidth();
  public abstract int getHeight();
  public abstract boolean isNull(); // isNull() == (getHeight() == 0)
  public abstract boolean isSingleLine(); // isSingleLine() == (getHeight() <= 1)
  public abstract boolean isEmpty(); // isEmpty() == (getWidth() == 0)

  @Override
  public String toString() {
    return DocStringBuilder.build(this);
  }
}
