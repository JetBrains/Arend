package org.arend.naming.reference;

import javax.annotation.Nonnull;

public class LocalReferable implements Referable {
  private final String myName;

  public LocalReferable(String name) {
    myName = name;
  }

  @Nonnull
  @Override
  public String textRepresentation() {
    return myName;
  }

  public boolean isHidden() {
    return false;
  }
}
