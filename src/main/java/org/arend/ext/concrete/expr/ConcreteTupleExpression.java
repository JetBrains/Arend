package org.arend.ext.concrete.expr;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ConcreteTupleExpression extends ConcreteExpression {
  @NotNull List<? extends ConcreteExpression> getFields();
}
