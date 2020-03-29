package org.arend.extImpl;

import org.arend.core.definition.Definition;
import org.arend.library.Library;

public interface DefinitionRequester {
  default void request(Definition definition, Library library) {}

  DefinitionRequester INSTANCE = new DefinitionRequester() {};
}
