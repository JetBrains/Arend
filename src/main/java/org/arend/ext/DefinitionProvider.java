package org.arend.ext;

import org.arend.ext.core.definition.CoreDefinition;
import org.arend.ext.reference.ArendRef;

import javax.annotation.Nullable;

public interface DefinitionProvider {
  @Nullable
  CoreDefinition getDefinition(ArendRef ref);
}
