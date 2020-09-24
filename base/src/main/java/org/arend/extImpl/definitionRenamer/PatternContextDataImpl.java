package org.arend.extImpl.definitionRenamer;

import org.arend.core.expr.Expression;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.typechecking.PatternContextData;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PatternContextDataImpl implements PatternContextData {
  private Expression myExpectedType;
  private final Concrete.Pattern myPattern;

  public PatternContextDataImpl(Expression expectedType, Concrete.Pattern pattern) {
    myExpectedType = expectedType;
    myPattern = pattern;
  }

  @NotNull
  @Override
  public Concrete.Pattern getMarker() {
    return myPattern;
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
}
