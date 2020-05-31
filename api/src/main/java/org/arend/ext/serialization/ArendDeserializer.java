package org.arend.ext.serialization;

import org.arend.ext.core.definition.CoreDefinition;
import org.jetbrains.annotations.NotNull;

public interface ArendDeserializer {
  @NotNull CoreDefinition getDefFromIndex(int index) throws DeserializationException;
  <D extends CoreDefinition> @NotNull D getDefFromIndex(int index, Class<D> clazz) throws DeserializationException;
}
