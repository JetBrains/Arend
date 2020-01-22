package org.arend.ext.typechecking;

import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.concrete.ConcreteLevel;
import org.arend.ext.concrete.expr.ConcreteReferenceExpression;
import org.arend.ext.core.expr.CoreExpression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface ContextData {
  @Nonnull ConcreteReferenceExpression getReferenceExpression();
  @Nonnull List<? extends ConcreteArgument> getArguments();
  CoreExpression getExpectedType();
}
