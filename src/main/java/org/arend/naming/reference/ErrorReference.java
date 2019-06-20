package org.arend.naming.reference;

import org.arend.naming.error.NotInScopeError;
import org.arend.typechecking.error.local.LocalError;

import javax.annotation.Nonnull;

public class ErrorReference implements Referable {
  private final String myText;
  private final LocalError myError;

  public ErrorReference(Object data, Referable referable, int index, String name) {
    myText = name;
    myError = new NotInScopeError(data, referable, index, name);
  }

  public ErrorReference(Object data, String name) {
    myText = name;
    myError = new NotInScopeError(data, null, 0, name);
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
