package org.arend.ext.concrete.expr;

import org.arend.ext.concrete.ConcreteSourceNode;

import javax.annotation.Nonnull;

public interface ConcreteExpression extends ConcreteSourceNode {
  @Nonnull ConcreteExpression app(@Nonnull ConcreteExpression argument);
  @Nonnull ConcreteExpression appImp(@Nonnull ConcreteExpression argument);
}
