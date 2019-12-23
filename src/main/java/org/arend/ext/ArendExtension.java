package org.arend.ext;

import java.util.Map;

public interface ArendExtension {
  default void setDependencies(Map<String, ArendExtension> dependencies) {}
  default void initialize() {}
}
