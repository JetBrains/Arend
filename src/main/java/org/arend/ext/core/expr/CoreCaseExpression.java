package org.arend.ext.core.expr;

import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.core.elimtree.CoreElimBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface CoreCaseExpression {
  boolean isSCase();
  @NotNull CoreParameter getParameters();
  @NotNull CoreExpression getResultType();
  @Nullable CoreExpression getResultTypeLevel();
  @NotNull CoreElimBody getElimBody();
  @NotNull Collection<? extends CoreExpression> getArguments();
}
