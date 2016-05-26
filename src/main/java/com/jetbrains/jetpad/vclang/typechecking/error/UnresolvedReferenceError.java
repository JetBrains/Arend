package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class UnresolvedReferenceError extends TypeCheckingError {
  private final String myName;

  public UnresolvedReferenceError(Abstract.SourceNode expression, String name) {
    super("Unresolved reference", expression);
    myName = name;
  }

  @Override
  public String toString() {
    return printHeader() + getMessage() + ": " + myName;
  }
}
