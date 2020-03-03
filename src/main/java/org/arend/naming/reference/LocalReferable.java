package org.arend.naming.reference;

import org.jetbrains.annotations.NotNull;

public class LocalReferable implements Referable {
  private final String myName;

  public LocalReferable(String name) {
    myName = name;
  }

  @NotNull
  @Override
  public String textRepresentation() {
    return myName;
  }

  public boolean isHidden() {
    return false;
  }
}
