package org.arend.extImpl;

import org.arend.core.definition.Definition;

public interface DefinitionRequester {
  default void request(Definition definition) {}

  DefinitionRequester INSTANCE = new DefinitionRequester() {};
}
