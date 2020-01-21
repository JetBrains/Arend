package org.arend.ext;

import org.arend.ext.core.definition.CoreDefinition;
import org.arend.ext.reference.RawRef;

import javax.annotation.Nonnull;

public interface DefinitionProvider {
  @Nonnull
  <T extends CoreDefinition> T getDefinition(@Nonnull RawRef ref, Class<T> clazz);
}
