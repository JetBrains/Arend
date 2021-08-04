package org.arend.extImpl;

import org.arend.core.expr.Expression;
import org.arend.ext.concrete.ConcreteClause;
import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.concrete.expr.ConcreteClauses;
import org.arend.ext.concrete.expr.ConcreteCoclauses;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.typechecking.ContextData;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ContextDataImpl extends BaseContextDataImpl implements ContextData {
  private Concrete.Expression myExpression;
  private List<? extends ConcreteArgument> myArguments;
  private ConcreteCoclauses myCoclauses;
  private ConcreteClauses myClauses;
  private Object myUserData;

  public ContextDataImpl(Concrete.Expression expression, List<? extends ConcreteArgument> arguments, ConcreteCoclauses coclauses, ConcreteClauses clauses, Expression expectedType, Object userData) {
    super(expectedType);
    myExpression = expression;
    myArguments = arguments;
    myCoclauses = coclauses;
    myClauses = clauses;
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

  @Override
  public void setMarker(ConcreteExpression marker) {
    if (!(marker instanceof Concrete.Expression)) throw new IllegalArgumentException();
    myExpression = (Concrete.Expression) marker;
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
  public @Nullable ConcreteCoclauses getCoclauses() {
    return myCoclauses;
  }

  @Override
  public void setCoclauses(ConcreteCoclauses coclauses) {
    myCoclauses = coclauses;
  }

  @Override
  public @Nullable ConcreteClauses getClauses() {
    return myClauses;
  }

  @Override
  public void setClauses(@Nullable ConcreteClauses clauses) {
    myClauses = clauses;
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
