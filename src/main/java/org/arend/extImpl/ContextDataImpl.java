package org.arend.extImpl;

import org.arend.core.expr.Expression;
import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.typechecking.ContextData;
import org.arend.term.concrete.Concrete;

import javax.annotation.Nonnull;
import java.util.List;

public class ContextDataImpl implements ContextData {
  private final Concrete.ReferenceExpression myExpression;
  private final List<? extends ConcreteArgument> myArguments;
  private Expression myExpectedType;

  public ContextDataImpl(Concrete.ReferenceExpression expression, List<? extends ConcreteArgument> arguments, Expression expectedType) {
    myExpression = expression;
    myArguments = arguments;
    myExpectedType = expectedType;
  }

  @Nonnull
  @Override
  public Concrete.ReferenceExpression getReferenceExpression() {
    return myExpression;
  }

  @Nonnull
  @Override
  public List<? extends ConcreteArgument> getArguments() {
    return myArguments;
  }

  @Override
  public Expression getExpectedType() {
    return myExpectedType;
  }

  public void setExpectedType(Expression expectedType) {
    myExpectedType = expectedType;
  }
}
