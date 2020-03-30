package org.arend.ext.typechecking;

import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.concrete.expr.ConcreteReferenceExpression;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ContextData {
  @NotNull ConcreteReferenceExpression getReferenceExpression();
  @NotNull List<? extends ConcreteArgument> getArguments();
  void setArguments(@NotNull List<? extends ConcreteArgument> arguments);
  CoreExpression getExpectedType();
  void setExpectedType(CoreExpression expectedType);
}
