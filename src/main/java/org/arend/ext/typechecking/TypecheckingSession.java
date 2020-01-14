package org.arend.ext.typechecking;

import org.arend.ext.concrete.ConcreteExpression;

import javax.annotation.Nonnull;

public interface TypecheckingSession {
  @Nonnull CheckedExpression typecheck(@Nonnull ConcreteExpression expression);
}
