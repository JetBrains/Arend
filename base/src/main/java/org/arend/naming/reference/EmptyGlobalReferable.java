package org.arend.naming.reference;

import org.arend.ext.reference.Precedence;
import org.jetbrains.annotations.NotNull;

public class EmptyGlobalReferable implements GlobalReferable {
  private final String myName;

  public EmptyGlobalReferable(String name) {
    myName = name;
  }

  @Override
  public @NotNull Precedence getPrecedence() {
    return Precedence.DEFAULT;
  }

  @Override
  public @NotNull String textRepresentation() {
    return myName;
  }
}
