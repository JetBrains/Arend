package org.arend.ext.typechecking;

import org.arend.ext.concrete.ConcreteClause;
import org.arend.ext.concrete.expr.*;
import org.arend.ext.core.expr.CoreExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents the context information of the current position of a meta definition.
 */
public interface ContextData {
  /**
   * If the definition was explicitly invoked from code,
   * returns the reference expression corresponding to this invocation.
   */
  default ConcreteReferenceExpression getReferenceExpression() {
    ConcreteExpression marker = getMarker();
    return marker instanceof ConcreteReferenceExpression ? (ConcreteReferenceExpression) marker : null;
  }

  /**
   * A marker that can be used for error reporting.
   */
  @NotNull ConcreteExpression getMarker();

  /**
   * Returns the list of arguments passed to the meta definition.
   */
  @NotNull List<? extends ConcreteArgument> getArguments();

  void setArguments(@NotNull List<? extends ConcreteArgument> arguments);

  /**
   * Returns the list of coclauses passed to the meta definition.
   */
  @Nullable ConcreteCoclauses getCoclauses();

  void setCoclauses(@Nullable ConcreteCoclauses coclauses);

  /**
   * Returns the list of clauses passed to the meta definition.
   */
  @Nullable ConcreteClauses getClauses();

  void setClauses(@Nullable ConcreteClauses clauses);

  /**
   * Returns the expected type in the current context.
   * The meta definition should return an expression of this type.
   * It can be {@code null} if the expected type is unknown.
   */
  CoreExpression getExpectedType();

  void setExpectedType(@Nullable CoreExpression expectedType);

  Object getUserData();

  void setUserData(Object userData);
}
