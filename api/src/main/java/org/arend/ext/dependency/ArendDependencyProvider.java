package org.arend.ext.dependency;

import org.arend.ext.core.definition.CoreDefinition;
import org.arend.ext.module.LongName;
import org.arend.ext.module.ModulePath;
import org.jetbrains.annotations.NotNull;

/**
 * Provides access to definitions defined in the library.
 */
public interface ArendDependencyProvider extends ArendReferenceProvider {
  /**
   * @return a definition of type {@code clazz} with name {@code name} defined in module {@code module}.
   */
  @NotNull <T extends CoreDefinition> T getDefinition(@NotNull ModulePath module, @NotNull LongName name, Class<T> clazz);

  /**
   * Fills all the fields of {@code dependencyContainer} annotated with {@link Dependency} annotation.
   */
  void load(@NotNull Object dependencyContainer);
}
