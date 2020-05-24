package org.arend.ext;

import org.arend.ext.module.LongName;
import org.arend.ext.module.ModulePath;
import org.arend.ext.reference.MetaRef;
import org.arend.ext.reference.Precedence;
import org.arend.ext.typechecking.MetaDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
   *
   * @return a new reference corresponding to the meta definition or {@code null} if there was an error
   */
  MetaRef declare(@NotNull ModulePath module, @NotNull LongName name, @NotNull String description, @NotNull Precedence precedence, @Nullable MetaDefinition meta);

  MetaRef declare(@NotNull ModulePath module, @NotNull LongName name, @NotNull String description, @NotNull Precedence precedence, @Nullable String alias, @Nullable Precedence aliasPrecedence, @Nullable MetaDefinition meta);
}
