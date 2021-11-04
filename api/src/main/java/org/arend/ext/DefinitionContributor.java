package org.arend.ext;

import org.arend.ext.concrete.definition.ConcreteDefinition;
import org.arend.ext.module.LongName;
import org.arend.ext.module.ModulePath;
import org.arend.ext.reference.MetaRef;
import org.arend.ext.reference.Precedence;
import org.arend.ext.typechecking.DeferredMetaDefinition;
import org.arend.ext.typechecking.MetaDefinition;
import org.arend.ext.typechecking.MetaResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * DefinitionContributor is used to declare meta definitions defined in the extension.
 */
public interface DefinitionContributor {
  /**
   * Declares new concrete definition.
   */
  void declare(@NotNull ConcreteDefinition definition);

  /**
   * Declares new meta definition.
   *
   * @param module        the module where the definition will be put
   * @param name          the name of the definition
   * @param description   the textual description of the definition
   * @param precedence    the precedence of the definition
   * @param alias         the alias name of the definition
   * @param aliasPrec     the alias precedence of the definition
   * @param meta          the definition itself
   * @param resolver      the associated resolver of the definition
   *
   * @return a new reference corresponding to the meta definition or {@code null} if there was an error
   */
  MetaRef declare(@NotNull ModulePath module, @NotNull LongName name, @NotNull String description, @NotNull Precedence precedence, @Nullable String alias, @Nullable Precedence aliasPrec, @Nullable MetaDefinition meta, @Nullable MetaResolver resolver);

  default MetaRef declare(@NotNull ModulePath module, @NotNull LongName name, @NotNull String description, @NotNull Precedence precedence, @Nullable String alias, @Nullable Precedence aliasPrecedence, @Nullable MetaDefinition meta) {
    return declare(module, name, description, precedence, alias, aliasPrecedence, meta, meta instanceof MetaResolver ? (MetaResolver) meta : meta instanceof DeferredMetaDefinition && ((DeferredMetaDefinition) meta).deferredMeta instanceof MetaResolver ? (MetaResolver) ((DeferredMetaDefinition) meta).deferredMeta : null);
  }

  default MetaRef declare(@NotNull ModulePath module, @NotNull LongName name, @NotNull String description, @NotNull Precedence precedence, @Nullable MetaDefinition meta) {
    return declare(module, name, description, precedence, null, null, meta);
  }
}
