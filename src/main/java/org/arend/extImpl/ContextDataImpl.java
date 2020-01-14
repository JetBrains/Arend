package org.arend.extImpl;

import org.arend.ext.concrete.ConcreteExpression;
import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.reference.ArendRef;
import org.arend.ext.typechecking.ContextData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class ContextDataImpl implements ContextData {
  private final Map<ArendRef, CoreBinding> myBindings;
  private final Collection<? extends ConcreteExpression> myArguments;
  private final CoreExpression myExpectedType;

  public ContextDataImpl(Map<? extends ArendRef, ? extends CoreBinding> bindings, Collection<? extends ConcreteExpression> arguments, CoreExpression expectedType) {
    myBindings = Collections.unmodifiableMap(bindings);
    myArguments = arguments;
    myExpectedType = expectedType;
  }

  @Nonnull
  @Override
  public Map<? extends ArendRef, ? extends CoreBinding> getBindings() {
    return myBindings;
  }

  @Nonnull
  @Override
  public Collection<? extends ConcreteExpression> getArguments() {
    return myArguments;
  }

  @Nullable
  @Override
  public CoreExpression getExpectedType() {
    return myExpectedType;
  }
}
