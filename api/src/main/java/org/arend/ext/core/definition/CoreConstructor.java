package org.arend.ext.core.definition;

import org.arend.ext.core.body.CoreBody;
import org.arend.ext.core.context.CoreParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CoreConstructor extends CoreDefinition {
  @NotNull CoreDataDefinition getDataType();
  @NotNull CoreParameter getDataTypeParameters();
  @NotNull CoreParameter getAllParameters();
  @Nullable CoreBody getBody();
}
