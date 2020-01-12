package org.arend.ext;

import org.arend.ext.core.definition.CoreDefinition;
import org.arend.ext.reference.RawRef;

import javax.annotation.Nonnull;

public interface DefinitionProvider {
  @Nonnull CoreDefinition getDefinition(@Nonnull RawRef ref);
}
