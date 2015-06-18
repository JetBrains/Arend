package com.jetbrains.jetpad.vclang.term.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class NotInScopeError extends TypeCheckingError {
  private final String myName;

  public NotInScopeError(Abstract.PrettyPrintableSourceNode expression, String name) {
    super("Not in scope", expression, null);
    myName = name;
  }

  @Override
  public String toString() {
    return printPosition() + getMessage() + ": " + myName;
  }
}
