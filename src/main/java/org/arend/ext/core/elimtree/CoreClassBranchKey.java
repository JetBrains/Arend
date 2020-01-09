package org.arend.ext.core.elimtree;

import org.arend.ext.core.definition.CoreClassDefinition;

import javax.annotation.Nonnull;

public interface CoreClassBranchKey extends CoreBranchKey {
  @Nonnull CoreClassDefinition getClassDefinition();
}
