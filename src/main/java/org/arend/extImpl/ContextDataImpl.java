package org.arend.extImpl;

import org.arend.ext.concrete.ConcreteArgument;
import org.arend.ext.concrete.ConcreteLevel;
import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.reference.ArendRef;
import org.arend.ext.typechecking.ContextData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

public class ContextDataImpl implements ContextData {
  private final Map<? extends ArendRef, ? extends CoreBinding> myBindings;
  private final ConcreteLevel myPLevel;
  private final ConcreteLevel myHLevel;
  private final Collection<? extends ConcreteArgument> myArguments;
  private final CoreExpression myExpectedType;

  public ContextDataImpl(Map<? extends ArendRef, ? extends CoreBinding> bindings, ConcreteLevel pLevel, ConcreteLevel hLevel, Collection<? extends ConcreteArgument> arguments, CoreExpression expectedType) {
    myBindings = bindings;
    myPLevel = pLevel;
    myHLevel = hLevel;
    myArguments = arguments;
    myExpectedType = expectedType;
  }

  @Nonnull
  @Override
  public Map<? extends ArendRef, ? extends CoreBinding> getBindings() {
    return myBindings;
  }

  @Nullable
  @Override
  public ConcreteLevel getPLevel() {
    return myPLevel;
  }

  @Nullable
  @Override
  public ConcreteLevel getHLevel() {
    return myHLevel;
  }

  @Nonnull
  @Override
  public Collection<? extends ConcreteArgument> getArguments() {
    return myArguments;
  }

  @Nullable
  @Override
  public CoreExpression getExpectedType() {
    return myExpectedType;
  }
}
