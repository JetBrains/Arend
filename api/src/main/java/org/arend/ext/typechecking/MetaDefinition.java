package org.arend.ext.typechecking;

import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface MetaDefinition {
  default @Nullable ConcreteExpression getConcretePresentation(@NotNull List<? extends ConcreteArgument> arguments) {
    return null;
  }

  default  @Nullable ConcreteExpression checkAndGetConcretePresentation(@NotNull List<? extends ConcreteArgument> arguments) {
    return getConcretePresentation(arguments);
  }

  default @Nullable TypedExpression invokeMeta(@NotNull ExpressionTypechecker typechecker, @NotNull ContextData contextData) {
    return null;
  }

  default  @Nullable TypedExpression checkAndInvokeMeta(@NotNull ExpressionTypechecker typechecker, @NotNull ContextData contextData) {
    return invokeMeta(typechecker, contextData);
  }
}
