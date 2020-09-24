package org.arend.extImpl;

import org.arend.core.expr.Expression;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.typechecking.BaseContextData;
import org.jetbrains.annotations.Nullable;

public abstract class BaseContextDataImpl implements BaseContextData {
  protected Expression myExpectedType;

  public BaseContextDataImpl(Expression expectedType) {
    myExpectedType = expectedType;
  }

  public Expression getExpectedType() {
    return myExpectedType;
  }

  public void setExpectedType(@Nullable CoreExpression expectedType) {
    if (!(expectedType instanceof Expression)) {
      throw new IllegalArgumentException();
    }
    myExpectedType = (Expression) expectedType;
  }
}
