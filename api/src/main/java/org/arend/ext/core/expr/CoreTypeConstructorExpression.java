package org.arend.ext.core.expr;

import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.core.definition.CoreFunctionDefinition;
import org.arend.ext.core.level.CoreLevels;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CoreTypeConstructorExpression extends CoreExpression {
  @NotNull CoreFunctionDefinition getDefinition();
  @NotNull CoreLevels getLevels();
  int getClauseIndex();
  @NotNull List<? extends CoreExpression> getClauseArguments();
  @NotNull CoreExpression getArgument();

  @NotNull CoreExpression getArgumentType();
  @NotNull CoreParameter getParameters();
  @Override @NotNull CoreFunCallExpression computeType();
}
