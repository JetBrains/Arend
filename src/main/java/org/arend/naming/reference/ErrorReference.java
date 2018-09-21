package org.arend.naming.reference;

import org.arend.naming.error.NotInScopeError;
import org.arend.typechecking.error.local.LocalError;

import javax.annotation.Nonnull;

public class ErrorReference implements Referable {
  private final String myText;
  private final LocalError myError;

  public ErrorReference(Object data, Referable referable, String name) {
    myText = name;
    myError = new NotInScopeError(data, referable, name);
  }

  public ErrorReference(LocalError error, String name) {
    myText = name;
    myError = error;
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
