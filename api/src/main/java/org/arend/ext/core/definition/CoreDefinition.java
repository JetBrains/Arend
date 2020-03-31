package org.arend.ext.core.definition;

import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.NotNull;

public interface CoreDefinition {
  @NotNull ArendRef getRef();
  @NotNull CoreParameter getParameters();
}
