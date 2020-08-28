package org.arend.ext.core.expr;

import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.core.body.CoreElimBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface CoreCaseExpression {
  boolean isSCase();
  @NotNull CoreParameter getParameters();
  @NotNull CoreExpression getResultType();
  @Nullable CoreExpression getResultTypeLevel();
  @NotNull CoreElimBody getElimBody();
  @NotNull List<? extends CoreExpression> getArguments();
}
