package com.jetbrains.jetpad.vclang.error.doc;

import java.util.Collections;
import java.util.List;

public abstract class LineDoc extends Doc {
  @Override
  public final int getHeight() {
    return 1;
  }

  @Override
  public final boolean isNull() {
    return false;
  }

  @Override
  public final boolean isSingleLine() {
    return true;
  }

  @Override
  public final List<LineDoc> linearize() {
    return Collections.singletonList(this);
  }
}
