package com.jetbrains.jetpad.vclang.error.doc;

import java.util.List;

public abstract class Doc {
  public abstract <P, R> R accept(DocVisitor<? super P, ? extends R> visitor, P params);

  public abstract int getWidth();
  public abstract int getHeight();
  public abstract boolean isNull(); // isNull() == (getHeight() == 0)
  public abstract boolean isSingleLine(); // isSingleLine() == (getHeight() <= 1)
  public abstract boolean isEmpty(); // isEmpty() == (getWidth() == 0)

  public abstract List<LineDoc> linearize(int indent, boolean indentFirst); // linearize(n, b).size() == getHeight()

  public final List<LineDoc> linearize() {
    return linearize(0, false);
  }

  @Override
  public String toString() {
    return DocStringBuilder.build(this);
  }
}
