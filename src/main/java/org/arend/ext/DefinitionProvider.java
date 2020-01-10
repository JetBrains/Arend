package org.arend.ext;

import org.arend.ext.concrete.ArendRef;
import org.arend.ext.core.definition.CoreDefinition;

import javax.annotation.Nullable;

public interface DefinitionProvider {
  @Nullable
  CoreDefinition getDefinition(ArendRef ref);
}
