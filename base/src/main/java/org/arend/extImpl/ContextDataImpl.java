package org.arend.extImpl;

import org.arend.core.expr.Expression;
import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.typechecking.ContextData;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ContextDataImpl implements ContextData {
  private final Concrete.ReferenceExpression myExpression;
  private List<? extends ConcreteArgument> myArguments;
  private Expression myExpectedType;

  public ContextDataImpl(Concrete.ReferenceExpression expression, List<? extends ConcreteArgument> arguments, Expression expectedType) {
    myExpression = expression;
    myArguments = arguments;
    myExpectedType = expectedType;
  }

  @NotNull
  @Override
  public Concrete.ReferenceExpression getReferenceExpression() {
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

  public void setExpectedType(Expression expectedType) {
    myExpectedType = expectedType;
  }
}
