package org.arend.ext.concrete;

import org.arend.ext.concrete.expr.ConcreteExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ConcreteClause extends ConcreteSourceNode {
  @NotNull List<? extends ConcretePattern> getPatterns();
  @Nullable ConcreteExpression getExpression();
}
