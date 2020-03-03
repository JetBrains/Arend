package org.arend.ext.typechecking;

import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.concrete.expr.ConcreteReferenceExpression;
import org.arend.ext.core.expr.CoreExpression;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ContextData {
  @NotNull ConcreteReferenceExpression getReferenceExpression();
  @NotNull List<? extends ConcreteArgument> getArguments();
  CoreExpression getExpectedType();
}
