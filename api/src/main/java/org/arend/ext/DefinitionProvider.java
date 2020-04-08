package org.arend.ext;

import org.arend.ext.core.definition.CoreDefinition;
import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.Nullable;

/**
 * Provides access to core definitions.
 */
public interface DefinitionProvider {
  /**
   * Returns the definitions corresponding to the given reference.
   */
  @Nullable CoreDefinition getCoreDefinition(@Nullable ArendRef ref);
}
