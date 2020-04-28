package org.arend.ext.prettyprinting;

import org.arend.ext.module.LongName;
import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.Nullable;

/**
 * Used during pretty printing to rename definitions by adding a prefix to them.
 */
public interface DefinitionRenamer {
  /**
   * Returns a prefix for the given definition.
   *
   * @param ref   a reference to a definition.
   * @return a prefix for the given definition or {@code null} if the definition should not be renamed.
   */
  @Nullable LongName getDefinitionPrefix(ArendRef ref);
}
