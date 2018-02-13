package com.jetbrains.jetpad.vclang.library.resolver;

import com.jetbrains.jetpad.vclang.library.PersistableSourceLibrary;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;

import javax.annotation.Nullable;

public interface DefinitionLocator {
  /**
   * Finds the library containing a specified definition.
   *
   * @param referable the definition.
   *
   * @return the library containing the definition
   *         or null if the definition does not belong to a {@link PersistableSourceLibrary}.
   */
  @Nullable
  PersistableSourceLibrary resolve(GlobalReferable referable);
}
