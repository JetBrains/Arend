package org.arend.ext.core.definition;

import org.arend.ext.core.level.CoreSort;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CoreDataDefinition extends CoreDefinition {
  int getTruncatedLevel();
  CoreSort getSort();
  @NotNull List<? extends CoreConstructor> getConstructors();

  default boolean isTruncated() {
    return getTruncatedLevel() >= -1;
  }

  CoreConstructor findConstructor(@NotNull String name);
}
