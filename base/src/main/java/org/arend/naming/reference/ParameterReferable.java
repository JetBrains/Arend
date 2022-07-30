package org.arend.naming.reference;

import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;

public class ParameterReferable implements Referable {
  private final Concrete.GeneralDefinition myDefinition;
  private final Referable myOriginalReferable;
  private final int myLevel;

  public ParameterReferable(Concrete.GeneralDefinition definition, Referable originalReferable, int level) {
    myDefinition = definition;
    myOriginalReferable = originalReferable;
    myLevel = level;
  }

  @Override
  public @NotNull String textRepresentation() {
    return myOriginalReferable.textRepresentation();
  }

  public Concrete.GeneralDefinition getDefinition() {
    return myDefinition;
  }

  public Referable getReferable() {
    return myOriginalReferable;
  }

  public int getLevel() {
    return myLevel;
  }
}
