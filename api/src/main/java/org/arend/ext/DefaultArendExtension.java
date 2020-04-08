package org.arend.ext;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;

public class DefaultArendExtension implements ArendExtension {
  private Map<String, ArendExtension> dependencies = Collections.emptyMap();

  @Override
  public void setDependencies(@NotNull Map<String, ArendExtension> dependencies) {
    this.dependencies = dependencies;
  }
}
