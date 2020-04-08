package org.arend.ext;

import org.arend.ext.module.LongName;
import org.arend.ext.module.ModulePath;
import org.arend.ext.reference.Precedence;
import org.arend.ext.typechecking.MetaDefinition;
import org.jetbrains.annotations.NotNull;

/**
 * DefinitionContributor is used to declare meta definitions defined in the extension.
 */
public interface DefinitionContributor {
  /**
   * Declares new meta definition.
   *
   * @param module        the module where the definition will be put
   * @param name          the name of the definition
   * @param description   the textual description of the definition
   * @param precedence    the precedence of the definition
   * @param meta          the definition itself
   */
  void declare(@NotNull ModulePath module, @NotNull LongName name, @NotNull String description, @NotNull Precedence precedence, @NotNull MetaDefinition meta);
}
