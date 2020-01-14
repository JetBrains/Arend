package org.arend.ext;

import org.arend.ext.concrete.ConcreteFactory;
import org.arend.ext.module.ModuleScopeProvider;

import javax.annotation.Nonnull;
import java.util.Map;

public interface ArendExtension {
  default void declareDefinitions(@Nonnull DefinitionContributor contributor) {}

  default void setDependencies(@Nonnull Map<String, ArendExtension> dependencies) {}

  default void setPrelude(@Nonnull ArendPrelude prelude) {}

  default void setConcreteFactory(@Nonnull ConcreteFactory factory) {}

  default void setModuleScopeProvider(@Nonnull ModuleScopeProvider moduleScopeProvider) {}

  @Nonnull
  default ModuleScopeProvider getModuleScopeProvider() {
    return module -> null;
  }

  default void load(@Nonnull DefinitionProvider definitionProvider) {}
}
