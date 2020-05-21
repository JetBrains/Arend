package org.arend.ext.dependency;

import org.arend.ext.core.definition.CoreDefinition;
import org.arend.ext.module.LongName;
import org.arend.ext.module.ModulePath;
import org.jetbrains.annotations.NotNull;

/**
 * Provides access to definitions defined in the library.
 */
public interface ArendDependencyProvider {
  @NotNull <T extends CoreDefinition> T getDefinition(@NotNull ModulePath module, @NotNull LongName name, Class<T> clazz);
}
