package org.arend.frontend.reference;

import org.arend.core.context.binding.ParamLevelVariable;
import org.arend.frontend.parser.Position;
import org.arend.naming.reference.LevelReferable;
import org.jetbrains.annotations.NotNull;

public class ConcreteLevelReferable implements LevelReferable {
  private final Position myPosition;
  private final String myName;
  private ParamLevelVariable myLevelVariable;

  public ConcreteLevelReferable(Position position, String name) {
    myPosition = position;
    myName = name;
  }

  @Override
  public @NotNull Position getData() {
    return myPosition;
  }

  @Override
  public @NotNull String textRepresentation() {
    return myName;
  }

  @Override
  public ParamLevelVariable getLevelVariable() {
    return myLevelVariable;
  }

  @Override
  public void setLevelVariable(ParamLevelVariable levelVariable) {
    myLevelVariable = levelVariable;
  }
}
