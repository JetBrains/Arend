package org.arend.ext.concrete.expr;

import org.arend.ext.concrete.ConcreteClause;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ConcreteCaseExpression {
  boolean isSCase();
  @NotNull List<? extends ConcreteCaseArgument> getArguments();
  @Nullable ConcreteExpression getResultType();
  @Nullable ConcreteExpression getResultTypeLevel();
  @NotNull List<? extends ConcreteClause> getClauses();
}
