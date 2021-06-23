package org.arend.ext.core.level;

import org.arend.ext.core.definition.CoreDefinition;
import org.jetbrains.annotations.NotNull;

public interface CoreLevels {
  /**
   * @return the substitution for level parameters of {@code definition} constructed from these levels.
   */
  LevelSubstitution makeSubstitution(@NotNull CoreDefinition definition);
}
