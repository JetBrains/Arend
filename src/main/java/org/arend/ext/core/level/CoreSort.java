package org.arend.ext.core.level;

import org.jetbrains.annotations.Nullable;

public interface CoreSort {
  @Nullable CoreLevel getPLevel();
  @Nullable CoreLevel getHLevel();
  boolean isProp();
  boolean isSet();
}
