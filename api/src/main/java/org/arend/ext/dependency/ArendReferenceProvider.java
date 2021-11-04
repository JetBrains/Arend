package org.arend.ext.dependency;

import org.arend.ext.module.LongName;
import org.arend.ext.module.ModulePath;
import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.NotNull;

/**
 * Provides access to definitions defined in the library.
 */
public interface ArendReferenceProvider {
  /**
   * @return a reference to a generated module.
   */
  @NotNull ArendRef getGeneratedModuleReference(@NotNull ModulePath module);

  /**
   * @return a reference to the definition with name {@code name} defined in module {@code module}.
   */
  @NotNull ArendRef getReference(@NotNull ModulePath module, @NotNull LongName name);
}
