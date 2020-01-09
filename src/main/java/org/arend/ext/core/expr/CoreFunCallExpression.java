package org.arend.ext.core.expr;

import org.arend.ext.core.definition.CoreFunctionDefinition;
import org.arend.ext.core.level.CoreSort;

import javax.annotation.Nonnull;
import java.util.Collection;

public interface CoreFunCallExpression extends CoreExpression {
  @Nonnull CoreFunctionDefinition getDefinition();
  @Nonnull CoreSort getSortArgument();
  @Nonnull Collection<? extends CoreExpression> getDefCallArguments();
}
