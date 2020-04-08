package org.arend.ext.reference;

import org.arend.ext.typechecking.MetaDefinition;
import org.jetbrains.annotations.Nullable;

/**
 * A reference to a meta definition.
 */
public interface MetaRef extends ArendRef {
  @Nullable MetaDefinition getDefinition();
}
