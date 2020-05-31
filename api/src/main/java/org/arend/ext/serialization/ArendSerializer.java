package org.arend.ext.serialization;

import org.arend.ext.core.definition.CoreDefinition;
import org.jetbrains.annotations.NotNull;

public interface ArendSerializer {
  int getDefIndex(@NotNull CoreDefinition definition);
}
