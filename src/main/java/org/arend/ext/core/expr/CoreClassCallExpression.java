package org.arend.ext.core.expr;

import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.definition.CoreClassDefinition;
import org.arend.ext.core.definition.CoreClassField;
import org.arend.ext.core.level.CoreSort;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

public interface CoreClassCallExpression extends CoreExpression {
  @Nonnull CoreClassDefinition getDefinition();
  @Nonnull CoreSort getSortArgument();
  @Nonnull CoreBinding getThisBinding();
  @Nonnull Collection<? extends Map.Entry<? extends CoreClassField, ? extends CoreExpression>> getImplementations();

  @Nullable CoreExpression getAbsImplementationHere(@Nonnull CoreClassField field);
  boolean isImplementedHere(@Nonnull CoreClassField field);
  boolean isImplemented(@Nonnull CoreClassField field);
}
