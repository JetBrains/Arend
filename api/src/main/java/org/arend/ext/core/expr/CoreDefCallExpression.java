package org.arend.ext.core.expr;

import org.arend.ext.core.definition.CoreDefinition;
import org.arend.ext.core.level.CoreLevel;
import org.arend.ext.core.level.CoreLevels;
import org.arend.ext.core.level.CoreSort;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CoreDefCallExpression extends CoreExpression {
  @NotNull CoreDefinition getDefinition();
  @NotNull List<? extends CoreExpression> getDefCallArguments();
}
