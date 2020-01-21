package org.arend.extImpl;

import org.arend.ext.concrete.ConcreteArgument;
import org.arend.ext.concrete.ConcreteLevel;
import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.reference.ArendRef;
import org.arend.ext.typechecking.ContextData;
import org.arend.term.concrete.Concrete;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ContextDataImpl implements ContextData {
  private final Concrete.ReferenceExpression myExpression;
  private final List<? extends ConcreteArgument> myArguments;
  private CoreExpression myExpectedType;

  public ContextDataImpl(Concrete.ReferenceExpression expression, List<? extends ConcreteArgument> arguments, CoreExpression expectedType) {
    myExpression = expression;
    myArguments = arguments;
    myExpectedType = expectedType;
  }

  @Nonnull
  @Override
  public Concrete.ReferenceExpression getReferenceExpression() {
    return myExpression;
  }

  @Nullable
  @Override
  public Concrete.LevelExpression getPLevel() {
    return myExpression.getPLevel();
  }

  @Nullable
  @Override
  public Concrete.LevelExpression getHLevel() {
    return myExpression.getHLevel();
  }

  @Nonnull
  @Override
  public List<? extends ConcreteArgument> getArguments() {
    return myArguments;
  }

  @Override
  public CoreExpression getExpectedType() {
    return myExpectedType;
  }
}
