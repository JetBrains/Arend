package org.arend.naming.reference;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataLevelReferable implements LevelReferable {
  private final Object myData;
  private final String myName;
  private final boolean myPLevels;

  public DataLevelReferable(Object data, String name, boolean isPLevels) {
    myData = data;
    myName = name;
    myPLevels = isPLevels;
  }

  @Override
  public @Nullable Object getData() {
    return myData;
  }

  @Override
  public @NotNull Referable.RefKind getRefKind() {
    return myPLevels ? RefKind.PLEVEL : RefKind.HLEVEL;
  }

  @Override
  public @NotNull String textRepresentation() {
    return myName;
  }

  @Override
  public @NotNull Referable getUnderlyingReferable() {
    return myData instanceof Referable ? (Referable) myData : this;
  }
}
