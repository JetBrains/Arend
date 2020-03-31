package org.arend.ext;

import org.arend.ext.concrete.ConcreteFactory;
import org.arend.ext.module.ModuleScopeProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface ArendExtension {
  default void declareDefinitions(@NotNull DefinitionContributor contributor) {}

  default void setDependencies(@NotNull Map<String, ArendExtension> dependencies) {}

  default void setPrelude(@NotNull ArendPrelude prelude) {}

  default void setConcreteFactory(@NotNull ConcreteFactory factory) {}

  default void setModuleScopeProvider(@NotNull ModuleScopeProvider moduleScopeProvider) {}

  default void setDefinitionProvider(@NotNull DefinitionProvider definitionProvider) {}

  @NotNull
  default ModuleScopeProvider getModuleScopeProvider() {
    return module -> null;
  }

  default void load(@NotNull RawDefinitionProvider definitionProvider) {}
}
