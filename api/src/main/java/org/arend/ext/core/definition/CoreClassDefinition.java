package org.arend.ext.core.definition;

import org.arend.ext.core.expr.CoreAbsExpression;
import org.arend.ext.core.level.CoreSort;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CoreClassDefinition extends CoreDefinition {
  @NotNull Set<? extends CoreClassDefinition> getSuperClasses();
  @NotNull List<? extends CoreClassField> getPersonalFields();
  @NotNull Collection<? extends Map.Entry<? extends CoreClassField, ? extends CoreAbsExpression>> getImplemented();
  @NotNull Collection<? extends Map.Entry<? extends CoreClassField, ? extends CoreAbsExpression>> getOverriddenFields();
  @NotNull CoreSort getSort();
  boolean isRecord();
  @Nullable CoreClassField getClassifyingField();

  boolean isSubClassOf(@NotNull CoreClassDefinition classDefinition);
  boolean isImplemented(@NotNull CoreClassField field);
  @Nullable CoreAbsExpression getImplementation(@NotNull CoreClassField field);
  boolean isOverridden(@NotNull CoreClassField field);
  @Nullable CoreAbsExpression getOverriddenType(@NotNull CoreClassField field);
}
