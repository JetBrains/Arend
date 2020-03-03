package org.arend.ext.core.expr;

import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.definition.CoreClassDefinition;
import org.arend.ext.core.definition.CoreClassField;
import org.arend.ext.core.level.CoreSort;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

public interface CoreClassCallExpression extends CoreExpression {
  @NotNull CoreClassDefinition getDefinition();
  @NotNull CoreSort getSortArgument();
  @NotNull CoreBinding getThisBinding();
  @NotNull Collection<? extends Map.Entry<? extends CoreClassField, ? extends CoreExpression>> getImplementations();

  @Nullable CoreExpression getAbsImplementationHere(@NotNull CoreClassField field);
  boolean isImplementedHere(@NotNull CoreClassField field);
  boolean isImplemented(@NotNull CoreClassField field);
}
