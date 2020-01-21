package org.arend.ext.core.expr;

import org.arend.ext.core.definition.CoreConstructor;
import org.arend.ext.core.level.CoreSort;

import javax.annotation.Nonnull;
import java.util.List;

public interface CoreConCallExpression extends CoreExpression {
  @Nonnull CoreConstructor getDefinition();
  @Nonnull CoreSort getSortArgument();
  @Nonnull List<? extends CoreExpression> getDataTypeArguments();
  @Nonnull List<? extends CoreExpression> getDefCallArguments();
}
