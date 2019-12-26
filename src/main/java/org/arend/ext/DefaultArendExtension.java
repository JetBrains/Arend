package org.arend.ext;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;

public class DefaultArendExtension implements ArendExtension {
  private Map<String, ArendExtension> dependencies = Collections.emptyMap();

  @Override
  public void setDependencies(@Nonnull Map<String, ArendExtension> dependencies) {
    this.dependencies = dependencies;
  }
}
