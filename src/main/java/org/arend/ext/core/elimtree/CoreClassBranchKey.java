package org.arend.ext.core.elimtree;

import org.arend.ext.core.definition.CoreClassDefinition;
import org.jetbrains.annotations.NotNull;

public interface CoreClassBranchKey extends CoreBranchKey {
  @NotNull CoreClassDefinition getClassDefinition();
}
