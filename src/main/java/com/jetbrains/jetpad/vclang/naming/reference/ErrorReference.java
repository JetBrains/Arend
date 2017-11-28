package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.naming.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalError;

import javax.annotation.Nonnull;

public class ErrorReference implements Referable {
  private final String myText;
  private final LocalError myError;

  public ErrorReference(Object data, Referable referable, String name) {
    myText = name;
    myError = new NotInScopeError(data, referable, name);
  }

  public LocalError getError() {
    return myError;
  }

  @Nonnull
  @Override
  public String textRepresentation() {
    return myText;
  }
}
