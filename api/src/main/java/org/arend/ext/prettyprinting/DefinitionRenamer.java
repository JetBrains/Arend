package org.arend.ext.prettyprinting;

import org.arend.ext.module.LongReference;
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
   * @return all references in qualifier for the given definition or {@code null} if the definition should not be renamed.
   */
  @Nullable LongReference renameDefinition(ArendRef ref);
}
