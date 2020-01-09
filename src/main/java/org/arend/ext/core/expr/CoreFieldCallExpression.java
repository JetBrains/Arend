package org.arend.ext.core.expr;

import org.arend.ext.core.definition.CoreClassField;
import org.arend.ext.core.level.CoreSort;

import javax.annotation.Nonnull;

public interface CoreFieldCallExpression extends CoreExpression {
  @Nonnull CoreClassField getDefinition();
  @Nonnull CoreSort getSortArgument();
  @Nonnull CoreExpression getArgument();
}
