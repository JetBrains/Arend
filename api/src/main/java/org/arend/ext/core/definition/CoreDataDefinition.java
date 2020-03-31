package org.arend.ext.core.definition;

import org.arend.ext.core.level.CoreSort;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface CoreDataDefinition extends CoreDefinition {
  boolean isTruncated();
  CoreSort getSort();
  @NotNull Collection<? extends CoreConstructor> getConstructors();
}
