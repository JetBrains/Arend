package org.arend.ext.typechecking;

import org.arend.ext.concrete.ConcreteExpression;
import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.reference.ArendRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public interface TypecheckingSession {
  @Nullable CheckedExpression typecheck(@Nonnull ConcreteExpression expression);
  @Nullable CheckedExpression check(@Nonnull CoreExpression expression);
  void setBindings(@Nonnull Map<ArendRef, CoreBinding> context);
}
