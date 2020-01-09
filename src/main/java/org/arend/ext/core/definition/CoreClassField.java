package org.arend.ext.core.definition;

import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.core.expr.CoreExpression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface CoreClassField extends CoreDefinition {
  @Nonnull CoreClassDefinition getParentClass();
  @Nonnull CoreParameter getThisParameter();
  @Nonnull CoreExpression getResultType();
  @Nullable CoreExpression getTypeLevel();
  boolean isProperty();
}
