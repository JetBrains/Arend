package org.arend.ext.core.definition;

import org.arend.ext.core.expr.CoreAbsExpression;
import org.arend.ext.core.level.CoreSort;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface CoreClassDefinition extends CoreDefinition {
  @Nonnull Set<? extends CoreClassDefinition> getSuperClasses();
  @Nonnull Collection<? extends CoreClassField> getPersonalFields();
  @Nonnull Collection<? extends Map.Entry<? extends CoreClassField, ? extends CoreAbsExpression>> getImplemented();
  @Nonnull Collection<? extends Map.Entry<? extends CoreClassField, ? extends CoreAbsExpression>> getOverriddenFields();
  @Nonnull CoreSort getSort();
  boolean isRecord();
  @Nullable CoreClassField getClassifyingField();

  boolean isSubClassOf(@Nonnull CoreClassDefinition classDefinition);
  boolean isImplemented(@Nonnull CoreClassField field);
  @Nullable CoreAbsExpression getImplementation(@Nonnull CoreClassField field);
  boolean isOverridden(@Nonnull CoreClassField field);
  @Nullable CoreAbsExpression getOverriddenType(@Nonnull CoreClassField field);
}
