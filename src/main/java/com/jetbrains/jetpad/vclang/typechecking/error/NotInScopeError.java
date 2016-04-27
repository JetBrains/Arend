package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class NotInScopeError extends TypeCheckingError {
  private final String myName;

  public NotInScopeError(Abstract.SourceNode expression, String name) {
    super("Not in scope", expression);
    myName = name;
  }

  @Override
  public String toString() {
    return printHeader() + getMessage() + ": " + myName;
  }
}
