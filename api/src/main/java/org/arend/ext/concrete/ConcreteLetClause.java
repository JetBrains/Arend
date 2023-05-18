package org.arend.ext.concrete;

import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.concrete.pattern.ConcretePattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ConcreteLetClause extends ConcreteSourceNode {
  @NotNull ConcretePattern getPattern();
  @NotNull List<? extends ConcreteParameter> getParameters();
  @Nullable ConcreteExpression getResultType();
  @NotNull ConcreteExpression getTerm();
}
