package org.arend.ext.typechecking;

import org.arend.ext.concrete.ConcreteArgument;
import org.arend.ext.concrete.ConcreteExpression;
import org.arend.ext.concrete.ConcreteLevel;
import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.reference.ArendRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public interface ContextData {
  @Nonnull ConcreteExpression getReferenceExpression();
  @Nullable ConcreteLevel getPLevel();
  @Nullable ConcreteLevel getHLevel();
  @Nonnull List<? extends ConcreteArgument> getArguments();
  CoreExpression getExpectedType();
}
