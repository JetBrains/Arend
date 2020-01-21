package org.arend.ext.typechecking;

import org.arend.ext.concrete.ConcreteExpression;
import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.reference.ArendRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public interface ExpressionTypechecker {
  @Nonnull Map<? extends ArendRef, ? extends CoreBinding> getContext();
  @Nonnull ErrorReporter getErrorReporter();
  @Nullable CheckedExpression typecheck(@Nonnull ConcreteExpression expression);
  @Nullable CheckedExpression check(@Nonnull CoreExpression expression);
  boolean compare(@Nonnull CoreExpression expr1, @Nonnull CoreExpression expr2, @Nonnull CMP cmp, @Nullable ConcreteExpression marker);
}
