package org.arend.ext.concrete.definition;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.jetbrains.annotations.Nullable;

public interface ConcreteDefinition extends ConcreteSourceNode {
  @Nullable ConcreteLevelParameters getPLevelParameters();
  @Nullable ConcreteLevelParameters getHLevelParameters();
  void setPLevelParameters(@Nullable ConcreteLevelParameters parameters);
  void setHLevelParameters(@Nullable ConcreteLevelParameters parameters);
}
