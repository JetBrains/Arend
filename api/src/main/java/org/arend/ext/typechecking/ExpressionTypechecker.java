package org.arend.ext.typechecking;

import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface ExpressionTypechecker {
  enum Stage { BEFORE_SOLVER, BEFORE_LEVELS, AFTER_LEVELS }

  @NotNull Map<? extends ArendRef, ? extends CoreBinding> getContext();
  @NotNull ErrorReporter getErrorReporter();
  @Nullable CheckedExpression typecheck(@NotNull ConcreteExpression expression);
  @Nullable CheckedExpression check(@NotNull CoreExpression expression, @NotNull ConcreteSourceNode sourceNode);
  @NotNull CheckedExpression defer(@NotNull MetaDefinition meta, @NotNull ContextData contextData, @NotNull CoreExpression type, @NotNull Stage stage);
  boolean compare(@NotNull CoreExpression expr1, @NotNull CoreExpression expr2, @NotNull CMP cmp, @Nullable ConcreteExpression marker);
}
