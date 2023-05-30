package org.arend.ext.concrete.expr;

import org.arend.ext.typechecking.TypedExpression;
import org.jetbrains.annotations.NotNull;

public interface ConcreteCoreExpression extends ConcreteExpression {
  @NotNull TypedExpression getTypedExpression();
}
