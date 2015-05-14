package com.jetbrains.jetpad.vclang.term.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

public class NotInScopeError extends TypeCheckingError {
  public NotInScopeError(Abstract.Expression expression, List<String> names) {
    super("Not in scope", expression, names);
  }

  public String getName() {
    return ((Abstract.VarExpression) getExpression()).getName();
  }

  @Override
  public String toString() {
    return getMessage() + ": " + getName();
  }
}
