package com.jetbrains.jetpad.vclang.error.doc;

public abstract class LineDoc extends Doc {
  @Override
  public final int getHeight() {
    return 1;
  }

  @Override
  public boolean isNull() {
    return false;
  }

  @Override
  public final boolean isSingleLine() {
    return true;
  }
}
