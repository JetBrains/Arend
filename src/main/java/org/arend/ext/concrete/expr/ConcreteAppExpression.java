package org.arend.ext.concrete.expr;

import javax.annotation.Nonnull;
import java.util.List;

public interface ConcreteAppExpression extends ConcreteExpression {
  @Nonnull ConcreteExpression getFunction();
  @Nonnull List<? extends ConcreteArgument> getArguments();
}
