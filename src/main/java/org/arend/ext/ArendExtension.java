package org.arend.ext;

import javax.annotation.Nonnull;
import java.util.Map;

public interface ArendExtension {
  default void setDependencies(@Nonnull Map<String, ArendExtension> dependencies) {}
  default void initialize() {}
}
