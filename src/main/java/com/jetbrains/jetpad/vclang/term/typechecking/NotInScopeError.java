package com.jetbrains.jetpad.vclang.term.typechecking;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;

public class NotInScopeError extends TypeCheckingError {
  public NotInScopeError(Abstract.Expression expression) {
    super("Not in scope", expression);
  }

  public String getName() {
    return ((Abstract.VarExpression) getExpression()).getName();
  }

  @Override
  public String toString() {
    return getMessage() + ": " + getName();
  }
}
