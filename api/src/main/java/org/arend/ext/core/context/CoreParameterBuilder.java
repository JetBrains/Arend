package org.arend.ext.core.context;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.core.expr.CoreExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CoreParameterBuilder {
  @NotNull CoreParameter getFirst();
  @NotNull CoreParameter getLast();
  @NotNull CoreParameter addCopyFirst(@NotNull CoreParameter parameter);
  @NotNull CoreParameter addCopyLast(@NotNull CoreParameter parameter);
  @NotNull CoreParameter addFirst(boolean isExplicit, @Nullable String name, @NotNull CoreExpression type, @NotNull ConcreteSourceNode marker);
  @NotNull CoreParameter addLast(boolean isExplicit, @Nullable String name, @NotNull CoreExpression type, @NotNull ConcreteSourceNode marker);
}
