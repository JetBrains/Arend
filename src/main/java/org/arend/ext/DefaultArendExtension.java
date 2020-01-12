package org.arend.ext;

import org.arend.ext.module.ModuleScopeProvider;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;

public class DefaultArendExtension implements ArendExtension {
  private Map<String, ArendExtension> dependencies = Collections.emptyMap();
  private ModuleScopeProvider moduleScopeProvider;

  @Override
  public void setDependencies(@Nonnull Map<String, ArendExtension> dependencies) {
    this.dependencies = dependencies;
  }

  @Override
  public void setModuleScopeProvider(@Nonnull ModuleScopeProvider moduleScopeProvider) {
    this.moduleScopeProvider = moduleScopeProvider;
  }

  @Nonnull
  @Override
  public ModuleScopeProvider getModuleScopeProvider() {
    return moduleScopeProvider;
  }

  @Override
  public void load(@Nonnull DefinitionProvider definitionProvider) {

  }
}
