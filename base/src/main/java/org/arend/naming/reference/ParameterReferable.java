package org.arend.naming.reference;

import org.jetbrains.annotations.NotNull;

public class ParameterReferable implements Referable {
  private final TCDefReferable myDefinition;
  private final int myIndex;
  private final Referable myOriginalReferable;

  public ParameterReferable(TCDefReferable definition, int index, Referable originalReferable) {
    myDefinition = definition;
    myIndex = index;
    myOriginalReferable = originalReferable;
  }

  @Override
  public @NotNull String textRepresentation() {
    return myOriginalReferable.textRepresentation();
  }

  public TCDefReferable getDefinition() {
    return myDefinition;
  }

  public int getIndex() {
    return myIndex;
  }

  public Referable getReferable() {
    return myOriginalReferable;
  }
}
