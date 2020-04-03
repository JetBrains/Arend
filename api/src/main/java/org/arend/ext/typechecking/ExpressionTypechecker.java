package org.arend.ext.typechecking;

import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.definition.CoreClassDefinition;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.core.expr.UncheckedExpression;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.error.ErrorReporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public interface ExpressionTypechecker {
  enum Stage { BEFORE_SOLVER, BEFORE_LEVELS, AFTER_LEVELS }

  @NotNull List<CoreBinding> getFreeBindingsList();
  @NotNull ErrorReporter getErrorReporter();
  @Nullable TypedExpression typecheck(@NotNull ConcreteExpression expression, @Nullable CoreExpression expectedType);
  @Nullable TypedExpression check(@NotNull UncheckedExpression expression, @NotNull ConcreteSourceNode sourceNode);
  @Nullable TypedExpression defer(@NotNull MetaDefinition meta, @NotNull ContextData contextData, @NotNull CoreExpression type, @NotNull Stage stage);
  boolean compare(@NotNull UncheckedExpression expr1, @NotNull UncheckedExpression expr2, @NotNull CMP cmp, @Nullable ConcreteExpression marker, boolean allowEquations, boolean normalize);
  <T> T withErrorReporter(@NotNull ErrorReporter errorReporter, Function<ExpressionTypechecker, T> action);
  @Nullable ConcreteExpression findInstance(@NotNull CoreClassDefinition classDefinition, @Nullable UncheckedExpression classifyingExpression, @NotNull ConcreteSourceNode sourceNode);
  @Nullable TypedExpression findInstance(@NotNull CoreClassDefinition classDefinition, @Nullable UncheckedExpression classifyingExpression, @Nullable CoreExpression expectedType, @NotNull ConcreteSourceNode sourceNode);
}
