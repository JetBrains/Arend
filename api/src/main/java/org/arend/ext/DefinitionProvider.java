package org.arend.ext;

import org.arend.ext.core.definition.CoreDefinition;
import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.Nullable;

public interface DefinitionProvider {
  @Nullable CoreDefinition getCoreDefinition(@Nullable ArendRef ref);
}
