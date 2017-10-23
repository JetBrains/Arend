package com.jetbrains.jetpad.vclang.term;

import javax.annotation.Nullable;

public interface ChildGroup extends Group {
  @Nullable ChildGroup getParentGroup();
}
