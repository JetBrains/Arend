package com.jetbrains.jetpad.vclang.module.serialization;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.util.LongName;

import javax.annotation.Nullable;

/**
 * Provides the information about the location of a definition.
 */
public interface DefinitionContextProvider {
  /**
   * Gets the module to which a specified definition belongs.
   *
   * @param referable a definition.
   *
   * @return the module of the definition
   *         or null if either the definition does not belong to this provider or some error occurred.
   */
  @Nullable
  ModulePath getDefinitionModule(GlobalReferable referable);

  /**
   * Gets the long name of a definition.
   *
   * @param referable a definition.
   *
   * @return the long name of the definition
   *         or null if either the definition does not belong to this provider or some error occurred.
   */
  @Nullable
  LongName getDefinitionFullName(GlobalReferable referable);
}
