package org.arend.ext.prettyprinting;

import org.arend.ext.module.LongName;
import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.Nullable;

/**
 * Used during pretty printing to rename definitions.
 */
public interface DefinitionRenamer {
  /**
   * Renames a definition.
   *
   * @param ref   a reference to a definition.
   * @return a full name for the given definition or {@code null} if the definition should not be renamed.
   */
  @Nullable LongName renameDefinition(ArendRef ref);
}
