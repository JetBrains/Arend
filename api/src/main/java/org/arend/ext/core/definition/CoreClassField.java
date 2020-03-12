package org.arend.ext.core.definition;

import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.core.expr.CoreExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CoreClassField extends CoreDefinition {
  @NotNull CoreClassDefinition getParentClass();
  @NotNull CoreParameter getThisParameter();
  @NotNull CoreExpression getResultType();
  @Nullable CoreExpression getTypeLevel();
  boolean isProperty();
}
