package org.arend.ext.core.definition;

import org.arend.ext.core.body.CoreBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CoreConstructor extends CoreDefinition {
  @NotNull CoreDataDefinition getDataType();
  @Nullable CoreBody getBody();
}
