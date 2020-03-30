package org.arend.ext.reference;

import org.arend.ext.typechecking.MetaDefinition;
import org.jetbrains.annotations.Nullable;

public interface MetaRef extends ArendRef {
  @Nullable MetaDefinition getDefinition();
}
