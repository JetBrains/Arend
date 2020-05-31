package org.arend.ext.typechecking;

import org.arend.ext.core.definition.CoreDefinition;
import org.jetbrains.annotations.NotNull;

public interface DefinitionListener {
  default void typechecked(@NotNull CoreDefinition definition) {}
  default void loaded(@NotNull CoreDefinition definition) {}
}
