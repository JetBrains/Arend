package org.arend.naming.reference;

import org.arend.ext.reference.Precedence;
import org.jetbrains.annotations.NotNull;

public class GlobalReferableImpl implements GlobalReferable {
  private final String myName;
  private final Precedence myPrecedence;

  public GlobalReferableImpl(String myName, Precedence myPrecedence) {
    this.myName = myName;
    this.myPrecedence = myPrecedence;
  }

  @Override
  public @NotNull Precedence getPrecedence() {
    return myPrecedence;
  }

  @Override
  public @NotNull Kind getKind() {
    return Kind.OTHER;
  }

  @Override
  public @NotNull String textRepresentation() {
    return myName;
  }
}
