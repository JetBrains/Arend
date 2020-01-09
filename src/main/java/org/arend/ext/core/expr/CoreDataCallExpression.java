package org.arend.ext.core.expr;

import org.arend.ext.core.definition.CoreDataDefinition;
import org.arend.ext.core.level.CoreSort;

import javax.annotation.Nonnull;
import java.util.Collection;

public interface CoreDataCallExpression extends CoreExpression {
  @Nonnull CoreDataDefinition getDefinition();
  @Nonnull CoreSort getSortArgument();
  @Nonnull Collection<? extends CoreExpression> getDefCallArguments();
}
