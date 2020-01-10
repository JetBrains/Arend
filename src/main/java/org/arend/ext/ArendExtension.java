package org.arend.ext;

import org.arend.ext.module.ModuleScopeProvider;

import javax.annotation.Nonnull;
import java.util.Map;

public interface ArendExtension {
  default void setDependencies(@Nonnull Map<String, ArendExtension> dependencies) {}

  default void setPrelude(@Nonnull ArendPrelude prelude) {}

  default void setModuleScopeProvider(@Nonnull ModuleScopeProvider moduleScopeProvider) {}

  @Nonnull
  default ModuleScopeProvider getModuleScopeProvider() {
    return module -> null;
  }

  default void setDefinitionProvider(@Nonnull DefinitionProvider definitionProvider) {}

  default void initialize() {}
}
