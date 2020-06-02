package org.arend.extImpl;

import org.arend.core.expr.Expression;
import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.concrete.expr.ConcreteReferenceExpression;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.typechecking.ContextData;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ContextDataImpl implements ContextData {
  private final Concrete.Expression myExpression;
  private List<? extends ConcreteArgument> myArguments;
  private Expression myExpectedType;
  private Object myUserData;

  public ContextDataImpl(Concrete.Expression expression, List<? extends ConcreteArgument> arguments, Expression expectedType, Object userData) {
    myExpression = expression;
    myArguments = arguments;
    myExpectedType = expectedType;
    myUserData = userData;
  }

  @Override
  public Concrete.ReferenceExpression getReferenceExpression() {
    return myExpression instanceof Concrete.ReferenceExpression ? (Concrete.ReferenceExpression) myExpression : null;
  }

  @NotNull
  @Override
  public Concrete.Expression getMarker() {
    return myExpression;
  }

  @NotNull
  @Override
  public List<? extends ConcreteArgument> getArguments() {
    return myArguments;
  }

  @Override
  public void setArguments(@NotNull List<? extends ConcreteArgument> arguments) {
    myArguments = arguments;
  }

  @Override
  public Expression getExpectedType() {
    return myExpectedType;
  }

  @Override
  public void setExpectedType(@Nullable CoreExpression expectedType) {
    if (!(expectedType instanceof Expression)) {
      throw new IllegalArgumentException();
    }
    myExpectedType = (Expression) expectedType;
  }

  @Override
  public Object getUserData() {
    return myUserData;
  }

  @Override
  public void setUserData(Object userData) {
    myUserData = userData;
  }
}
