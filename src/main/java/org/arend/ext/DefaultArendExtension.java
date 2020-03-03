package org.arend.ext;

import org.arend.ext.module.ModuleScopeProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;

public class DefaultArendExtension implements ArendExtension {
  private Map<String, ArendExtension> dependencies = Collections.emptyMap();
  private ModuleScopeProvider moduleScopeProvider;

  @Override
  public void setDependencies(@NotNull Map<String, ArendExtension> dependencies) {
    this.dependencies = dependencies;
  }

  @Override
  public void setModuleScopeProvider(@NotNull ModuleScopeProvider moduleScopeProvider) {
    this.moduleScopeProvider = moduleScopeProvider;
  }

  @NotNull
  @Override
  public ModuleScopeProvider getModuleScopeProvider() {
    return moduleScopeProvider;
  }
}
