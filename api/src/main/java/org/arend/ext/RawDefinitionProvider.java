package org.arend.ext;

import org.arend.ext.core.definition.CoreDefinition;
import org.arend.ext.reference.RawRef;
import org.jetbrains.annotations.NotNull;

public interface RawDefinitionProvider {
  @NotNull <T extends CoreDefinition> T getDefinition(@NotNull RawRef ref, Class<T> clazz);
}
