package org.arend.ext.core.expr;

import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.core.definition.CoreClassDefinition;
import org.arend.ext.core.definition.CoreClassField;
import org.arend.ext.typechecking.TypedExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

public interface CoreClassCallExpression extends CoreDefCallExpression {
  @Override @NotNull CoreClassDefinition getDefinition();
  @NotNull CoreBinding getThisBinding();
  @NotNull Collection<? extends Map.Entry<? extends CoreClassField, ? extends CoreExpression>> getImplementations();

  @Nullable CoreExpression getAbsImplementationHere(@NotNull CoreClassField field);
  @Nullable UncheckedExpression getImplementationHere(@NotNull CoreClassField field, @NotNull CoreExpression thisExpr);
  @Nullable UncheckedExpression getImplementation(@NotNull CoreClassField field, @NotNull CoreExpression thisExpr);
  @Nullable CoreExpression getImplementationHere(@NotNull CoreClassField field, @NotNull TypedExpression thisExpr);
  @Nullable CoreExpression getImplementation(@NotNull CoreClassField field, @NotNull TypedExpression thisExpr);
  @Nullable CoreExpression getClosedImplementation(@NotNull CoreClassField field);
  boolean isImplementedHere(@NotNull CoreClassField field);
  boolean isImplemented(@NotNull CoreClassField field);
  @NotNull CoreParameter getClassFieldParameters();
}
